/*

	JENKINS IOS PIPELINE
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
        // Global variable map
        _MAP = [debugMode                     : debugMode,
                plGitPath                     : plGitPath,
                plGitBranch                   : plGitBranch,
                plGitCredentials              : plGitCredentials,
                pyjiPath                      : cp.map['pyjiPath'],
                jiraURL                       : cp.map['jiraURL'],
                jiraUsername                  : cp.map['jiraUsername'],
                jiraPassword                  : cp.map['jiraPassword'],
                jiraProject                   : jiraProject,
                jiraAssignee                  : jiraAssignee,

                // Project Related
                gitPath                       : gitPath,
                gitBranch                     : gitBranch,
                gitCredentials                : gitCredentials,
                versionFilePath               : versionFilePath,

                appName                       : appName.replaceAll("\\s", ""),
                oSName                        : "iOS",
                slaveLabel                    : slaveLabel,
                fileNameExt                   : "ipa",
                compileCmd                    : compileCmd,
                targetWorkspace               : targetWorkspace,
                targetProject                 : targetProject,
                targetEnvironment             : targetEnvironment,
                deployEnvironment             : "Fabric",
                artifactFolder                : "build/Build/Artifacts",
                signCertHash                  : cp.map['signCertHash'],
                podInstallRequired            : podInstallRequired,
                distributeCmd                 : distributeCmd,
                fabricOrganizationApiKey      : fabricOrganizationApiKey,
                fabricOrganizationSecretKey   : fabricOrganizationSecretKey,

                targetTest                    : targetTest,
                targetLive                    : targetLive,
                targetClean                   : targetClean,
                snapshotDistributionRecipients: snapshotDistributionRecipients,
                releaseDistributionRecipients : releaseDistributionRecipients,
                releaseDescriptorFile         : "",

                slackHookURL                  : cp.map['slackHookURL'],
                slackChannel                  : slackChannel,


        ]

        def fields = []
    } // stage ('Init')

    node(slaveLabel) {

        ct.log("Debug mode is " + _MAP['debugMode'])

        // Prepare the filesystem variables
        _MAP['workspace'] = pwd() + "/"

        // Was job started by changes in git or was it manually started?
        _MAP['jobStartedAutomatically'] = ct.isStartedByGitPush()

        // STAGE____________________________________________________________________________________________
        stage('Pull') {
            // 	‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾

            // Checkout project codebase
            git branch: _MAP['gitBranch'], credentialsId: _MAP['gitCredentials'], url: _MAP['gitPath']

            // Get latest snapshot/release
            _MAP['gitLastSnapshotTag'] = ct.getLatestSnapshotGitTag()
            _MAP['gitLastReleaseTag'] = ct.getLatestReleaseGitTag()

            // Get commit messages since last snapshot. However if this is the first snapshot after a release then issues returned are since latest release
            _MAP['gitLogSubjectsSinceLastSnapshot'] = ct.getGitLogSubjectsByTag(_MAP['gitLastSnapshotTag'])

            // Get commit messages since last release
            _MAP['gitLogSubjectsSinceLastRelease'] = ct.getGitLogSubjectsByTag(_MAP['gitLastReleaseTag'])

        } // stage ('Pull')
        // STAGE____________________________________________________________________________________________
        stage('Version') {
            // 	‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾
            _MAP['versionShort'] = cmt.getFieldValueFromPlistFile(cp.map['plistBuddyPath'], _MAP['versionFilePath'], "CFBundleShortVersionString")
            _MAP['revisionShort'] = ct.getGitHEADShortRevision()
            _MAP['revision'] = ct.getGitHEADLongRevision()
            _MAP['branchIncrement'] = ct.getGitCommitIncrementInBranch(_MAP['gitBranch'])
            _MAP['releaseState'] = "SNAPSHOT"

            // Create Version Name
            _MAP['version'] = ct.getVersion(_MAP)

            // Set Version inside Plist file
            cmt.setFieldValueInPlistFile(cp.map['plistBuddyPath'], _MAP['versionFilePath'], "CFBundleShortVersionString", _MAP['version'])

            // Edit Jenkins current build Description/Name
            def jobName = _MAP['gitBranch'] == "develop" ? _MAP['version'] : _MAP['version'] + " (" + _MAP['gitBranch'] + ")" 
            !_MAP['jobStartedAutomatically'] ? ct.setCurrentBuildDisplayName(jobName) : ct.setCurrentBuildDisplayName("* " + jobName)

        } // stage ('Version')

        // STAGE____________________________________________________________________________________________
        stage('Build') {
            // 	‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾
            // Run pod install?
            if (_MAP['podInstallRequired'] == "true") {
                ct.runCommand(cp.map['podInstallerPath'] + " install --no-ansi")
            }

            // Unlock OSX Keychain
            cmt.unlockOSXKeychain(cp.map['osxSecurityPath'], cp.map['osxKeychainPath'], cp.map['osxKeychainPassword'])
            ct.runCommand("mkdir -p " + _MAP['artifactFolder'])
            ct.runCommand("rm -f " + _MAP['artifactFolder'] + "/*.ipa")

            // Compose compile command
            def finalCompileCmd = sprintf(compileCmd,
                    cp.map['xcodeBuildPath'],
                    _MAP['targetWorkspace'],
                    (_MAP['targetEnvironment'].equalsIgnoreCase("LIVE") ? _MAP['targetLive'] : _MAP['targetTest']) + " clean archive",
                    _MAP['artifactFolder'] + "/" + ct.getBinaryFilename(_MAP) + ".xcarchive")

            try {

                // Compile
                ct.runCommand(finalCompileCmd)

                // Export Archive to IPA file
                ct.runCommand(cp.map['xcodeBuildPath']
                        + ' -exportArchive -exportFormat ipa -archivePath ' + _MAP['artifactFolder'] + '/' + ct.getBinaryFilename(_MAP) + '.xcarchive'
                        + ' -exportPath ' + _MAP['artifactFolder'] + '/' + ct.getBinaryFilename(_MAP) + '.' + _MAP['fileNameExt'] + " -exportWithOriginalSigningIdentity")
                // ct.runCommand(cp.map['xcRunPath'] + ' -sdk iphoneos PackageApplication -v "build/Build/Products/Debug-iphoneos/'
                // 	+ _MAP['targetProject'] + '.app" -o "'
                // 	+ _MAP['workspace'] + _MAP['artifactFolder'] + '/'+ ct.getBinaryFilename(_MAP) + '.' + _MAP['fileNameExt'] +'"')

                // } catch (hudson.AbortException e) {

                //        // Notify via slack
                // 	fields = []
                //    	fields.add(["title" : "git Path", "value" : _MAP['gitPath'], "short" : false])
                //    	fields.add(["title" : "git Branch", "value" : _MAP['gitBranch'], "short" : false])
                //    	fields.add(["title" : "git Revision", "value" : _MAP['revision'], "short" : false])
                //    	fields.add(["title" : "Aborted Command", "value" : finalCompileCmd, "short" : false])
                //    	fields.add(["title" : "Latest Commited Tickets", "value" : _MAP['gitLogSubjectsSinceLastSnapshot']?_MAP['gitLogSubjectsSinceLastSnapshot']:"n/a", "short" : false])

                //     ct.logS(_MAP['slackHookURL'],
                //     				_MAP['slackChannel'],
                //     				_MAP['appName'] + " (" + _MAP['version'] + ") on '" + env.NODE_NAME + "' node",
                //     				env.BUILD_URL,
                //     				"Build compilation ABORTED."
                //     					 + " (<" + env.BUILD_URL + "console|Logs> | <" + env.BUILD_URL + "parameters|Parameters>)",
                //     				"Build compilation ABORTED " + _MAP['appName'] + " (" + _MAP['version']  + ") on '" + env.NODE_NAME + "' node",
                //     				"warning",
                //     				fields)

                //     // Show current runtime vars
                // 	ct.printMapVars(_MAP)

                //        throw e

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
                        "Build compilation FAILED after a commit by "
                                + "<mailto:" + ct.getLastCommitterEmail() + "|" + ct.getLastCommitterName() + ">"
                                + " (<" + env.BUILD_URL + "console|Logs> | <" + env.BUILD_URL + "parameters|Parameters>)",
                        "Build compilation FAILED " + _MAP['appName'] + " (" + _MAP['version'] + ") on '" + env.NODE_NAME + "' node",
                        "danger",
                        fields)

                // Show current runtime vars
                ct.printMapVars(_MAP)
                throw e
            }
        } // stage ('Build')
        // STAGE____________________________________________________________________________________________
        stage('Test (Auto)') {
            // 	‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾

        }
        if (!_MAP['jobStartedAutomatically']) {

            // STAGE____________________________________________________________________________________________
            stage('Deploy (SNAPSHOT)') {
                // 	‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾

                // Compose snapshot distribution command
                def finalSnapshotDistributionCmd = sprintf(distributeCmd, cp.map['crashlyticsSubmitPath'], _MAP['fabricOrganizationApiKey'], _MAP['fabricOrganizationSecretKey'], _MAP['snapshotDistributionRecipients'], ct.getDistributionNotesFilename(_MAP['releaseState']), _MAP['artifactFolder'] + '/' + ct.getBinaryFilename(_MAP) + '.' + _MAP['fileNameExt'])

                try {

                    // Create Distribution Notes file
                    ct.createFabricDistributionNotesFile(_MAP['releaseState'], _MAP['version'], ct.prepareDistributionNotesFile(_MAP['releaseState'], _MAP['gitLogSubjectsSinceLastSnapshot'], _MAP['gitLogSubjectsSinceLastRelease'], _MAP['version'], _MAP['gitBranch']))

                    // Upload Snapshot to Crashlytics
                    ct.runCommand(finalSnapshotDistributionCmd)

                    // Tag SNAPSHOT and push the tag to the branch
                    ct.gitTag(_MAP['version'])

                    // Update JIRA tickets
                    ct.updateJIRAIssuesFromLog(_MAP['gitLogSubjectsSinceLastSnapshot'], _MAP['version'])

                    // Create JIRA ticket
                    if (_MAP['gitLogSubjectsSinceLastRelease'])
                        ct.createJIRAIssue(_MAP['jiraProject'], _MAP['version'], _MAP['version'], "Simplified Task", _MAP['jiraAssignee'], ct.jiraIssuesToMap(_MAP['gitLogSubjectsSinceLastRelease']))

                    // Archive Binary file
                    ct.log("Archiving binary '" + _MAP['artifactFolder'] + '/' + ct.getBinaryFilename(_MAP) + '.' + _MAP['fileNameExt'] + "' ...")
                    archive '**/*' + ct.getBinaryFilename(_MAP) + '.' + _MAP['fileNameExt']

                } catch (e) {
                    // Notify in slack
                    fields = []
                    fields.add(["title": "Deploy Environment", "value": _MAP['deployEnvironment'], "short": true])
                    fields.add(["title": "Failed Command", "value": finalSnapshotDistributionCmd, "short": false])
                    fields.add(["title": "Latest Commited Tickets", "value": _MAP['gitLogSubjectsSinceLastRelease'] ? _MAP['gitLogSubjectsSinceLastRelease'] : "n/a", "short": false])

                    ct.logS(_MAP['slackHookURL'],
                            _MAP['slackChannel'],
                            _MAP['appName'] + " (" + _MAP['version'] + ") on '" + env.NODE_NAME + "' node",
                            env.BUILD_URL,
                            _MAP['releaseState'] + " deployment FAILED on " + _MAP['deployEnvironment']
                                    + " (<" + env.BUILD_URL + "console|Logs> | <" + env.BUILD_URL + "parameters|Parameters>)",
                            _MAP['releaseState'] + " deployment FAILED on '" + env.NODE_NAME + "' node",
                            "danger",
                            fields)

                    // Show current runtime vars
                    ct.printMapVars(_MAP)

                    throw e
                }

                // Notify in Slack
                fields = []
                fields.add(["title": "Operating System", "value": _MAP['oSName'], "short": true])
                fields.add(["title": "Target Environment", "value": _MAP['targetEnvironment'], "short": true])
                fields.add(["title": "Assigned To", "value": _MAP['jiraAssignee'], "short": true])
                fields.add(["title": "Recipients", "value": _MAP['snapshotDistributionRecipients'], "short": true])
                fields.add(["title": "Committed Tickets Since Last Release", "value": _MAP['gitLogSubjectsSinceLastRelease'] ? _MAP['gitLogSubjectsSinceLastRelease'] : "n/a", "short": false])

                ct.logS(_MAP['slackHookURL'],
                        _MAP['slackChannel'],
                        _MAP['appName'] + " (" + _MAP['version'] + ") on '" + env.NODE_NAME + "' node",
                        env.BUILD_URL,
                        _MAP['releaseState'] + " deployment completed successfuly!",
                        _MAP['releaseState'] + " deployment completed successfuly!",
                        "good",
                        fields)


            } // stage ('Deploy (SNAPSHOT)')

            // STAGE____________________________________________________________________________________________
            stage('Deploy (RELEASE)') {
                // 	‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾

                // Print internal variables
                ct.printMapVars(_MAP)

                inputID = "DeployRelease"
                question = "Proceed with deploying Release for '" + _MAP['versionShort'] + "' ?"

                def userInput = input(
                        id: inputID,
                        message: question,
                        parameters: [
                                [$class: 'StringParameterDefinition', defaultValue: _MAP['releaseDistributionRecipients'], description: 'The Fabric group that will receive the RELEASE on their phones ', name: 'ReleaseRecipients'],
                                [$class: 'BooleanParameterDefinition', defaultValue: false, description: 'Prepare Store Release package?', name: 'PrepStoreReleasePkg']
                        ])

                _MAP['releaseDistributionRecipients'] = userInput['ReleaseRecipients']

                snapshotFilename = ct.getBinaryFilename(_MAP)

                // Update Version name
                _MAP['releaseState'] = "RELEASE"
                _MAP['version'] = ct.getVersion(_MAP)

                // Edit Jenkins current build Description/Name
                jobName = _MAP['gitBranch'] == "develop" ? _MAP['version'] : _MAP['version'] + " (" + _MAP['gitBranch'] + ")" 
                ct.setCurrentBuildDisplayName(jobName)

                // Unlock keychain once again
                cmt.unlockOSXKeychain(cp.map['osxSecurityPath'], cp.map['osxKeychainPath'], cp.map['osxKeychainPassword'])

                // Modify xcarchive
                cmt.changeArchiveVersion(_MAP['artifactFolder'] + '/' + snapshotFilename + '.xcarchive', _MAP['version'])

                // Export
                ct.runCommand(cp.map['xcodeBuildPath']
                        + ' -exportArchive -exportFormat ipa -archivePath ' + _MAP['artifactFolder'] + '/' + snapshotFilename + '.xcarchive'
                        + ' -exportPath ' + _MAP['artifactFolder'] + '/' + ct.getBinaryFilename(_MAP) + '.' + _MAP['fileNameExt'] + " -exportWithOriginalSigningIdentity")

                // Compose release distribution command
                def finalReleaseDistributionCmd = sprintf(distributeCmd, cp.map['crashlyticsSubmitPath'], _MAP['fabricOrganizationApiKey'], _MAP['fabricOrganizationSecretKey'], _MAP['releaseDistributionRecipients'], ct.getDistributionNotesFilename(_MAP['releaseState']), _MAP['workspace'] + _MAP['artifactFolder'] + '/' + ct.getBinaryFilename(_MAP) + '.' + _MAP['fileNameExt'])

                try {

                    // Create Distribution Notes file
                    ct.createFabricDistributionNotesFile(_MAP['releaseState'], _MAP['version'], ct.prepareDistributionNotesFile(_MAP['releaseState'], _MAP['gitLogSubjectsSinceLastSnapshot'], _MAP['gitLogSubjectsSinceLastRelease'], _MAP['version'], _MAP['gitBranch']))

                    // Upload Release to Crashlytics
                    ct.runCommand(finalReleaseDistributionCmd)

                    // Proceed with tagging the RELEASE
                    ct.gitTag(_MAP['version'])

                    // Update Version file and commit to git
                    def newVersion = ct.incrementVersionByOne(_MAP['versionShort'])
                    cmt.updateVersionFile(newVersion, _MAP['versionFilePath'])
                    ct.gitCommitPush(_MAP['versionFilePath'], "Jenkins Automatic Version Increment from '" + _MAP['versionShort'] + "' to '" + newVersion + "'", _MAP['gitBranch'])

                    // Update JIRA tickets
                    ct.updateJIRAIssuesFromLog(_MAP['gitLogSubjectsSinceLastRelease'], _MAP['version'])

                    // Archive Binary file
                    ct.log("Archiving binary '" + _MAP['artifactFolder'] + '/' + ct.getBinaryFilename(_MAP) + '.' + _MAP['fileNameExt'] + "' ...")
                    archive '**/*' + ct.getBinaryFilename(_MAP) + '.' + _MAP['fileNameExt']

                    // Write Release descriptor File
                    _MAP['releaseDescriptorFile'] = _MAP['workspace'] + ct.getBinaryFilename(_MAP) + ".desc"
                    writeFile file: _MAP['releaseDescriptorFile'], text: ct.mapToPropString(_MAP)

                } catch (e) {
                    // Notify in slack
                    fields = []
                    fields.add(["title": "Deploy Environment", "value": _MAP['deployEnvironment'], "short": true])
                    fields.add(["title": "Failed Command", "value": finalReleaseDistributionCmd, "short": false])
                    fields.add(["title": "Latest Commited Tickets", "value": _MAP['gitLogSubjectsSinceLastRelease'] ? _MAP['gitLogSubjectsSinceLastRelease'] : "n/a", "short": false])

                    ct.logS(_MAP['slackHookURL'],
                            _MAP['slackChannel'],
                            _MAP['appName'] + " (" + _MAP['version'] + ") on '" + env.NODE_NAME + "' node",
                            env.BUILD_URL,
                            _MAP['releaseState'] + " deployment FAILED on " + _MAP['deployEnvironment']
                                    + " (<" + env.BUILD_URL + "console|Logs> | <" + env.BUILD_URL + "parameters|Parameters>)",
                            _MAP['releaseState'] + " deployment FAILED on '" + env.NODE_NAME + "' node",
                            "danger",
                            fields)

                    // Show current runtime vars
                    ct.printMapVars(_MAP)
                    throw e
                }

                // Notify in Slack
                fields = []
                fields.add(["title": "Operating System", "value": _MAP['oSName'], "short": true])
                fields.add(["title": "Target Environment", "value": _MAP['targetEnvironment'], "short": true])
                fields.add(["title": "Approved By", "value": _MAP['jiraAssignee'], "short": true])
                fields.add(["title": "Recipients", "value": _MAP['snapshotDistributionRecipients'], "short": true])
                fields.add(["title": "Committed Tickets Since Last Release", "value": _MAP['gitLogSubjectsSinceLastRelease'] ? _MAP['gitLogSubjectsSinceLastRelease'] : "n/a", "short": false])

                ct.logS(_MAP['slackHookURL'],
                        _MAP['slackChannel'],
                        _MAP['appName'] + " (" + _MAP['version'] + ") on '" + env.NODE_NAME + "' node",
                        env.BUILD_URL,
                        _MAP['releaseState'] + " deployment completed successfuly!",
                        _MAP['releaseState'] + " deployment completed successfuly!",
                        "good",
                        fields)

            } // stage ('Deploy (RELEASE)')

        } else { // workaround so that the visualization doesnt refresh
            ct.log("Skipping the deployment stages since job was started via BitBucket push")
            stage('Deploy (SNAPSHOT)') {}
            stage('Deploy (RELEASE)') {}
        }

        // STAGE____________________________________________________________________________________________
        stage('Cleanup') {
            // 	‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾
            ct.setCurrentBuildDescription(ct.createDescription(_MAP))

            // Print internal variables
            ct.printMapVars(_MAP)
        } // stage ('Cleanup')

    } // node
} // start


return this;
