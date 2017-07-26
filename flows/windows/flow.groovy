/*

	JENKINS ANDROID PIPELINE
	by Iason Dimitrakopoulos and Aristotelis Siempis
	idimitrakopoulos@gmail.com


*/




def start(plGitPath, plGitBranch, plGitCredentials) {

	// Get all external groovy resources
	def tk
	fileLoader.withGit(
	    plGitPath, 
	    plGitBranch, 
	    plGitCredentials, '') {
	    tk = fileLoader.load('common/toolkit.groovy');
	    tm = fileLoader.load('common/templates.groovy');
	}


	// STAGE____________________________________________________________________________________________
	stage 'Init'
	// 	‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾
	// Global variable map
	_MAP = [debugMode: debugMode,
			UNIQUE_RUNID: tk.getUUID(), 
			plGitPath: plGitPath, 
			plGitBranch: plGitBranch, 
			plGitCredentials: plGitCredentials,
			pyjiPath: pyjiPath,
			jiraURL: jiraURL,
			jiraUsername: jiraUsername,
			jiraPassword: jiraPassword,
			jiraProject: jiraProject,
			jiraAssignee: jiraAssignee,

			// Project Related
			gitPath: gitPath, 
			gitBranch: gitBranch, 
			gitCredentials: gitCredentials,
			versionFilePath: versionFilePath,

			appName: appName.replaceAll("\\s",""),
			oSName: "Android",
			compileCmd: compileCmd,
			targetEnvironment: targetEnvironment, 
			deployEnvironment: "Fabric", 
			signStoreRelease: signStoreRelease,

			targetTestSnapshot: targetTestSnapshot,
			targetTestRelease: targetTestRelease,
			targetLiveSnapshot: targetLiveSnapshot,
			targetLiveRelease: targetLiveRelease,
			targetLiveStoreRelease: targetLiveStoreRelease,
			targetLiveStoreReleaseUnsigned: targetLiveStoreReleaseUnsigned,
			targetClean: targetClean,
			snapshotDistributionRecipients: snapshotDistributionRecipients,
			releaseDistributionRecipients: releaseDistributionRecipients,

			slackHookURL: "https://hooks.slack.com/services/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",
			slackChannel: slackChannel,


			]

			def fields = []



	node ('Microsoft') {

		tk.log("Debug mode is " + _MAP['debugMode'])

		// Prepare the filesystem variables
		_MAP['workspace'] = pwd() + "/"

		// Was job started by changes in git or was it manually started?
		_MAP['jobStartedAutomatically'] = tk.isStartedByGitPush()

		// STAGE____________________________________________________________________________________________
		stage 'Pull'
		// 	‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾
		
		// Checkout project codebase
		git branch: _MAP['gitBranch'], credentialsId: _MAP['gitCredentials'], url: _MAP['gitPath']

		// Get latest snapshot/release
		_MAP['gitLastSnapshotTag'] = tk.getLatestSnapshotGitTag()
		_MAP['gitLastReleaseTag'] = tk.getLatestReleaseGitTag()

		// Get commit messages since last snapshot. However if this is the first snapshot after a release then issues returned are since latest release
		_MAP['gitLogSubjectsSinceLastSnapshot'] = tk.getGitLogSubjectsByTag(_MAP['gitLastSnapshotTag'])

		// Get commit messages since last release
		_MAP['gitLogSubjectsSinceLastRelease'] = tk.getGitLogSubjectsByTag(_MAP['gitLastReleaseTag'])

		// STAGE____________________________________________________________________________________________
		stage 'Version'
		// 	‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾
		_MAP['versionShort'] = tk.getCodeVersion(_MAP['versionFilePath'])
		_MAP['revisionShort'] = tk.getGitHEADShortRevision()
		_MAP['revision'] = tk.getGitHEADLongRevision()
		_MAP['branchIncrement'] = tk.getGitCommitIncrementInBranch(_MAP['gitBranch'])
		_MAP['releaseState'] = "SNAPSHOT"

		// Create Version Name
		_MAP['version'] = tk.getVersion(_MAP)


		// Edit Jenkins current build Description/Name
		!_MAP['jobStartedAutomatically']?tk.setCurrentBuildDisplayName(_MAP['version']):tk.setCurrentBuildDisplayName("* " + _MAP['version'])


		// STAGE____________________________________________________________________________________________
		//stage 'Build'
		// 	‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾
		
		// Compose compile command
		
		
		/*rmdir /S Output

		bat 'C:\Nuget\nuget.exe restore Source/WindScorecardSolution.sln'

		Command Line Arguments

		/p:Configuration=Release
		/p:DeployOnBuild=True
		/p:DeployDefaultTarget=WebPublish
		/p:WebPublishMethod=FileSystem
		/p:DeleteExistingFiles=True
		/p:publishUrl=$WORKSPACE\Builds



		zip artifacts
		set /p Version=<Source/Manifest.txt
		"C:\Program Files\7-Zip\7z.exe" a -tzip Output\web-%Version%-r%SVN_REVISION%-SNAPSHOT.zip .\Builds\*

		Archive artifacts
		Output/*.zip
		*/

		// STAGE____________________________________________________________________________________________
		stage 'Cleanup'
		// 	‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾

		dmap = [version: _MAP['version'], 
				headrev: tk.getGitHEADLongRevision(),  
				releaseState: _MAP['releaseState'],
				snapshotSince: tk.getLatestSnapshotGitTag(), 
				gitLogSubjectsSinceLastSnapshot: _MAP['gitLogSubjectsSinceLastSnapshot'],
				releaseSince: tk.getLatestReleaseGitTag(), 				
				gitLogSubjectsSinceLastRelease: _MAP['gitLogSubjectsSinceLastRelease'],

				]


		tk.setCurrentBuildDescription(tk.createDescription(dmap))


		// Print internal variables
		tk.printMapVars(_MAP)

	} // node
} // start


return this;
