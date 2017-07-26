/*

	JENKINS ANDROID STORE PIPELINE
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
        // 	‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾
        def _MAP = []
        def fields = []

        // Remove spaces if any
        releaseDescriptorFile = releaseDescriptorFile.trim()

        ct.log("Descriptor file provided: " + releaseDescriptorFile)


    } // Stage 'Init'

    node {

        if (!fileExists(releaseDescriptorFile)) {
            throw new Exception("File " + releaseDescriptorFile + " doesnt exist in the filesystem")
        }

        // Read properties from descriptor file
        _MAP = readProperties file: releaseDescriptorFile

        if (!_MAP['targetEnvironment'] == "LIVE") {
            throw new Exception("You are trying to submit a client that was released with " + _MAP("targetEnvironment") + " services ...")
        }

        _MAP['targetLiveStoreReleaseSigned'] = targetLiveStoreReleaseSigned
        _MAP['targetLiveStoreReleaseUnsigned'] = targetLiveStoreReleaseUnsigned
        _MAP['uploadBinaryToStore'] = uploadBinaryToStore

        ct.log("Debug mode is " + _MAP['debugMode'])

        // STAGE____________________________________________________________________________________________
        stage('Pull') {
            // 	‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾

            // Checkout project codebase
            git branch: _MAP['gitBranch'], credentialsId: _MAP['gitCredentials'], url: _MAP['gitPath']

            // Checkout proper tag
            ct.checkoutSpecificGitTag(_MAP['version'])


        } // Stage 'Pull'

        // STAGE____________________________________________________________________________________________
        stage('Version') {
            // 	‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾

            // Identify whether it will be signed or not
            _MAP['releaseState'] = signStoreRelease == "true" ? "SRs" : "SRu"

            // Create Version Name
            _MAP['version'] = ct.getVersion(_MAP)

            // Edit Jenkins current build Description/Name
            ct.setCurrentBuildDisplayName(ct.getBinaryFilename(_MAP))

        } // Stage 'Version'

        // STAGE____________________________________________________________________________________________
        stage('Approvals') {
            // 	‾

            // Print internal variables
            ct.printMapVars(_MAP)

            ct.log("Approvals section for " + _MAP['version'])

            // Get version code as it will be after compilation
            incVersionCode = cmt.getIncrementedVersionCode()
            versionStore = cmt.getVersionStore(_MAP)

            inputID = "approveVersionCodeVersionStore"
            question = "The app that will be submitted will have versionCode = '" + incVersionCode.toString() + "' and version = '" + versionStore + "'. I have checked that versionCode currently in the store for this app is a smaller number and that the version is correct."

            userInput = input(
                    id: inputID,
                    message: question,
                    submitter: qaApprovers
            )

            inputID = "hasBeenAlreadyListed"
            question = "I have verified that there is at least 1 previous listing of the app in the play store with the same app id"

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
            // 	‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾

            // Compose compile command
            def action = cmt.determineStoreReleaseAction(uploadBinaryToStore, releaseAssetsFile)
            def target = signStoreRelease == "true" ? targetLiveStoreReleaseSigned : targetLiveStoreReleaseUnsigned

            def finalCompileCmd = sprintf(_MAP['compileCmd'], ct.getBinaryFilename(_MAP), cmt.getVersionStore(_MAP), _MAP['targetClean'], action + target)

            try {

                // Compile
                ct.runCommand(finalCompileCmd)

                // Tag STORE-RELEASE and push the tag to the branch
                ct.gitTag(_MAP['version'])

                // Archive Binary file
                ct.log("Archiving binary '" + ct.getBinaryFilename(_MAP) + '.' + _MAP['fileNameExt'] + "' ...")
                archive '**/*' + ct.getBinaryFilename(_MAP) + '.' + _MAP['fileNameExt']

                // Commit changed versionCode file
                // ct.gitCommitPush(cp.map['versionPropertiesPath'], "Automated Jenkins increment due to Store Release", _MAP['gitBranch'])

				// Notify in Slack
				fields = []
			    fields.add(["title" : "Operating System", "value" : _MAP['oSName'], "short" : true])
			    fields.add(["title" : "Target Environment", "value" : _MAP['targetEnvironment'], "short" : true])

				ct.logS(_MAP['slackHookURL'], 
				  				_MAP['slackChannel'], 
				   				_MAP['appName'] + " (" + _MAP['version'] + ") on '" + env.NODE_NAME + "' node", 
				   				env.BUILD_URL, 
				   				_MAP['releaseState'] + " deployment completed successfuly!",
				   				_MAP['releaseState'] + " deployment completed successfuly!", 
				   				"good", 
				   				fields)

            } catch (Exception e) {
                // Notify Culprit via email
                // mail subject: _MAP['appName'] + " (" + _MAP['version'] + ") build FAILED", to: ct.getLastCommitterEmail(), body: "Hello " + ct.getLastCommitterName() + ",\n\nJenkins thinks that you broke the build with your last commit. Can you check please?"

                // Notify Culprit via slack
                fields = []
                fields.add(["title": "git Path", "value": _MAP['gitPath'], "short": false])
                fields.add(["title": "git Branch", "value": _MAP['gitBranch'], "short": false])
                fields.add(["title": "git Revision", "value": _MAP['revision'], "short": false])
                fields.add(["title": "Failed Command", "value": finalCompileCmd, "short": false])

                ct.logS(_MAP['slackHookURL'],
                        _MAP['slackChannel'],
                        _MAP['appName'] + " (" + _MAP['version'] + ") on '" + env.NODE_NAME + "' node",
                        env.BUILD_URL,
                        "Build compilation FAILED " + " (<" + env.BUILD_URL + "console|Logs> | <" + env.BUILD_URL + "parameters|Parameters>)",
                        "Build compilation FAILED " + _MAP['appName'] + " (" + _MAP['version'] + ") on '" + env.NODE_NAME + "' node",
                        "danger",
                        fields)

                // Show current runtime vars
                ct.printMapVars(_MAP)
                throw e
            }
        } // Stage 'Deploy'

        // STAGE____________________________________________________________________________________________
        stage('Cleanup') {
            // 	‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾
            ct.setCurrentBuildDescription(ct.createDescription(_MAP))

            // Print internal variables
            ct.printMapVars(_MAP)

        } // Stage 'Cleanup'

    } // node
} // start


return this;
