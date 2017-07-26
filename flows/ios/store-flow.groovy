/*

	JENKINS IOS STORE PIPELINE
	by Iason Dimitrakopoulos
	idimitrakopoulos@gmail.com


*/


def start(plGitPath, plGitBranch, plGitCredentials) {

    // Get all external groovy resources
    fileLoader.withGit(
            plGitPath,
            plGitBranch,
            plGitCredentials, '') {
        ct = fileLoader.load('common/toolkit.groovy');
        cp = fileLoader.load('common/properties.groovy');
        cmt = fileLoader.load('common/mobile/mobile-toolkit.groovy');
    }

    // STAGE____________________________________________________________________________________________
    stage('Init') {

        def _MAP = []
        def fields = []

        // Remove spaces if any
        releaseDescriptorFile = releaseDescriptorFile.trim()

        ct.log("Descriptor file provided: " + releaseDescriptorFile)

    }// STAGE 'Init'


    node(slaveLabel) {

        if (!fileExists(releaseDescriptorFile)) {
            throw new Exception("File " + releaseDescriptorFile + " doesnt exist in the filesystem")
        }

        // Read properties from descriptor file
        _MAP = readProperties file: releaseDescriptorFile

        if (!_MAP['targetEnvironment'] == "LIVE") {
            throw new Exception("You are trying to submit a client that was released with " + _MAP("targetEnvironment") + " services ...")
        }

        _MAP['targetRelease'] = targetRelease

        ct.log("Debug mode is " + _MAP['debugMode'])

        // STAGE____________________________________________________________________________________________
        stage('Pull') {

            // Checkout project codebase
            git branch: _MAP['gitBranch'], credentialsId: _MAP['gitCredentials'], url: _MAP['gitPath']

            // Checkout proper tag
            ct.checkoutSpecificGitTag(_MAP['version'])

        } // Stage 'Pull'

        // STAGE____________________________________________________________________________________________
        stage('Version') {

            // Create Version Name
            _MAP['version'] = ct.getVersion(_MAP)

            // Set Version inside Plist file
            cmt.setFieldValueInPlistFile(cp.map['plistBuddyPath'], _MAP['versionFilePath'], "CFBundleShortVersionString", _MAP['versionShort'])
            cmt.setFieldValueInPlistFile(cp.map['plistBuddyPath'], _MAP['versionFilePath'], "CFBundleVersion", cmt.getVersionStoreIOS(_MAP))

            // Edit Jenkins current build Description/Name
            ct.setCurrentBuildDisplayName(ct.getBinaryFilename(_MAP))

        } // Stage 'Version'

        // STAGE____________________________________________________________________________________________
        stage('Approvals') {

            // Print internal variables
            ct.printMapVars(_MAP)

            ct.log("Approvals section for " + _MAP['version'])

            inputID = "approveVersionCodeVersionStore"
            question = "The app that will be submitted will have versionCode = '" + cmt.getVersionStoreIOS(_MAP) + "'. I have checked that versionCode currently in the store for this app is a smaller number and that the version is correct."

            userInput = input(
                    id: inputID,
                    message: question,
                    submitter: qaApprovers
            )

            inputID = "hasBeenAlreadyListed"
            question = "I have verified that there is at least 1 previous listing of the app in the app store with the same bundle id: " + appIdentifier

            userInput = input(
                    id: inputID,
                    message: question,
                    submitter: qaApprovers
            )

            inputID = "submissionApproval"
            question = "I have received approval by both customer and QA for the specific version to be submitted " + _MAP['version']

            userInput = input(
                    id: inputID,
                    message: question,
                    submitter: pmApprovers
            )

            inputID = "finalCheck"
            question = "Proceeding... Wait! Are you sure?"

            userInput = input(
                    id: inputID,
                    message: question,
                    submitter: pmApprovers
            )

            ct.log("Approvals stage completed. Proceeding ...")

        } // Stage 'Approvals'

        // STAGE____________________________________________________________________________________________
        stage('Deploy') {

            // Run pod install?
            if (_MAP['podInstallRequired'] == "true") {
                ct.runCommand(cp.map['podInstallerPath'] + " install --no-ansi")
            }

            // Unlock OSX Keychain
            cmt.unlockOSXKeychain(cp.map['osxSecurityPath'], cp.map['osxKeychainPath'], cp.map['osxKeychainPassword'])
            ct.runCommand("mkdir -p " + _MAP['artifactFolder'])
            ct.runCommand("rm -rfv " + _MAP['artifactFolder'] + '/*')

            // Compose compile command
            def finalCompileCmd = sprintf(_MAP['compileCmd'],
                    cp.map['xcodeBuildPath'],
                    _MAP['targetWorkspace'],
                    (_MAP['targetEnvironment'].equalsIgnoreCase("LIVE") ? _MAP['targetLive'] : _MAP['targetTest']) + " clean archive",
                    _MAP['artifactFolder'] + "/" + ct.getBinaryFilename(_MAP) + ".xcarchive")

            _MAP['workspace'] = pwd() + "/"


            try {

                //create exportOptionsPlist
                def method = "app-store"
                ct.runCommand('defaults write ' + _MAP['workspace'] + 'exportOptions method ' + method)

                // Compile
                ct.runCommand(finalCompileCmd)

                // Export Archive to IPA file for App store
                ct.runCommand(cp.map['xcodeBuildPath']
                        + ' -exportArchive -exportOptionsPlist ' + _MAP['workspace'] + 'exportOptions.plist'
                        + ' -archivePath ' + _MAP['artifactFolder'] + '/' + ct.getBinaryFilename(_MAP) + '.xcarchive'
                        + ' -exportPath ' + _MAP['artifactFolder'] + '/' + ct.getBinaryFilename(_MAP))

                ct.runCommand('mv '  + _MAP['artifactFolder'] + '/' + ct.getBinaryFilename(_MAP) + '/' + _MAP['targetLive'] + '.' + _MAP['fileNameExt'] + " "
                        + _MAP['artifactFolder'] + '/' + ct.getBinaryFilename(_MAP) + '/' + ct.getBinaryFilename(_MAP) + '.' +  _MAP['fileNameExt'])


                if (uploadBinaryToStore != "true") {
                    ct.log("Archiving binary '" + _MAP['artifactFolder'] + '/' + ct.getBinaryFilename(_MAP) + '.xcarchive' + "' ...")
                    ct.runCommand('zip -r ' + _MAP['artifactFolder'] + '/' + ct.getBinaryFilename(_MAP) + '.zip ' + _MAP['artifactFolder'] + '/' + ct.getBinaryFilename(_MAP) + '.xcarchive')
                    archive _MAP['artifactFolder'] + '/' + ct.getBinaryFilename(_MAP) + '.zip'
                    archive _MAP['artifactFolder'] + '/' + ct.getBinaryFilename(_MAP) + '/' + ct.getBinaryFilename(_MAP) + '.' + _MAP['fileNameExt']
                }


            } catch (Exception e) {
                // Notify Culprit via email
                mail subject: _MAP['appName'] + " (" + _MAP['version'] + ") build FAILED", to: ct.getLastCommitterEmail(), body: "Hello " + ct.getLastCommitterName() + ",\n\nJenkins thinks that you broke the build with your last commit. Can you check please?"

                // Notify Culprit via slack
                fields = []
                fields.add(["title": "git Path", "value": _MAP['gitPath'], "short": false])
                fields.add(["title": "git Branch", "value": _MAP['gitBranch'], "short": false])
                fields.add(["title": "git Revision", "value": _MAP['revision'], "short": false])
                fields.add(["title": "Failed Command", "value": finalCompileCmd, "short": false])
                fields.add(["title": "Latest Commited Tickets", "value": _MAP['gitLogSubjectsSinceLastSnapshot'] ? _MAP['gitLogSubjectsSinceLastSnapshot'] : "n/a", "short": false])

                ct.logS(_MAP['slackHookURL'],
                        _MAP['slackChannel'],
                        _MAP['appName'] + " (" + _MAP['version'] + ") on '" + env.NODE_NAME + "' node",
                        env.BUILD_URL,
                        "Deploy FAILED after a commit by "
                                + "<mailto:" + ct.getLastCommitterEmail() + "|" + ct.getLastCommitterName() + ">"
                                + " (<" + env.BUILD_URL + "console|Logs> | <" + env.BUILD_URL + "parameters|Parameters>)",
                        "Build compilation FAILED " + _MAP['appName'] + " (" + _MAP['version'] + ") on '" + env.NODE_NAME + "' node",
                        "danger",
                        fields)

                // Show current runtime vars
                ct.printMapVars(_MAP)
                throw e
            }

        } // Stage 'Deploy'

        // STAGE____________________________________________________________________________________________
        stage('Submit') {

            if (uploadBinaryToStore == "true") {
                // Compose Submit command
                def submitFinalCommand = sprintf(submitCmd,
                        cp.map['osxDeliverPath'],
                        username,
                        appIdentifier,
                        _MAP['artifactFolder'] + '/' + ct.getBinaryFilename(_MAP) + '/' + ct.getBinaryFilename(_MAP) + '.' + _MAP['fileNameExt'],
                        "--app_version " + cmt.getVersionStoreIOS(_MAP)
                )
                ct.log("final submit command: " + submitFinalCommand)

                try {

                    ct.runCommand(submitFinalCommand)

                } catch (Exception e) {
                    // Notify Culprit via slack
                    fields = []
                    fields.add(["title": "submitFinalCommand", "value": submitFinalCommand, "short": false])

                    ct.logS(_MAP['appName'] + " (" + _MAP['version'] + ") on '" + env.NODE_NAME + "' node",
                            "Submit FAILED " + _MAP['appName'] + " (" + _MAP['version'] + ") on '" + env.NODE_NAME + "' node",
                            fields)

                    // Show current runtime vars
                    ct.printMapVars(_MAP)
                    throw e
                }
            }

        }// Stage 'Submit'

        // STAGE____________________________________________________________________________________________
        stage('Cleanup') {

            ct.setCurrentBuildDescription(ct.createDescription(_MAP))

            // Print internal variables
            ct.printMapVars(_MAP)

        } // Stage 'Cleanup'

    } // node

} // start


return this;
