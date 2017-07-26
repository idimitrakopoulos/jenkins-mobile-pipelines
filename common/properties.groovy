map = [
        // Generic
        pyjiPath             : "/var/lib/jenkins/PyJi/",
        jiraURL              : "https://exuscouk.atlassian.net",
        jiraUsername         : "jenkins",
        jiraPassword         : "password",
        slackHookURL         : "https://hooks.slack.com/services/XXXXXXXXXXXXXX/XXXXXXXXXXXX/XXXXXXXXXXXX",

        // iOS
        plistBuddyPath       : "/usr/libexec/PlistBuddy",
        xcodeBuildPath       : "/usr/bin/xcodebuild",
        xcodeCodesignPath    : "/usr/bin/codesign",
        osxDeliverPath       : "/usr/local/bin/deliver",
        osxSecurityPath      : "/usr/bin/security",
        osxKeychainPath      : "/Users/jenkins/Library/Keychains/jenkins.keychain",
        osxKeychainPassword  : "password",
        xcRunPath            : "/usr/bin/xcrun",
        podInstallerPath     : "/usr/local/bin/pod",
        crashlyticsSubmitPath: "Frameworks/Crashlytics.framework/submit",
        signCertHash         : "XXXXXXXXXXXXXXXXXXXXXXXXXXXXX",

        // Android
        versionPropertiesPath: "app/version.properties",

]
return this;