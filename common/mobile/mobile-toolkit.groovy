// Mobile Toolkit
def getFieldValueFromPlistFile(plistBuddyPath, plistFilePath, field) {
    return (ct.getCommandOutput(plistBuddyPath + ' -c "Print ' + field + '" ' + plistFilePath, ct.generatedUUID, false)).replaceAll("\n", "")
}

def setFieldValueInPlistFile(plistBuddyPath, plistFilePath, field, value) {
    ct.runCommand(plistBuddyPath + ' -c "Set :' + field + ' ' + value + '" ' + plistFilePath)
}

def unlockOSXKeychain(osxSecurityPath, osxKeychainPath, osxKeychainPassword) {
	ct.log("Unlocking OSX keychain " + osxKeychainPath)
    ct.runCommand(osxSecurityPath + ' unlock-keychain -p '+ osxKeychainPassword + ' ' + osxKeychainPath)
}

// def changeIpaVersion(ipaPath, ipaFilename, newVersion, newIpaFilename) {
// 	ct.log("Unpacking " + ipaFilename + " and modifying its version to " + newVersion)
// 	// Unzip IPA file
//     ct.runCommand("unzip " + ipaPath + "/" + ipaFilename + ".ipa") 
//     // Change version in *Info.plist
//     setFieldValueInPlistFile(cp.map['plistBuddyPath'], "Payload/*.app/*Info.plist", "CFBundleShortVersionString", newVersion)
//     // Remove current signature
//     ct.runCommand("rm -rf Payload/*.app/_CodeSignature Payload/*.app/CodeResources")
//     // Sign again
//     ct.runCommand(cp.map['xcodeCodesignPath'] + " --force --sign " + cp.map['signCertHash'] + " Payload/*.app")
//     // Repackage
//     ct.runCommand("zip -qr " + ipaPath + "/" + newIpaFilename + " Payload/")
//     // Cleanup
//     ct.runCommand("rm -rf Payload")

//     ct.log("Version modification complete. The new filename is " + ipaPath + "/" + newIpaFilename)
// }

def changeArchiveVersion(archivePath, newVersion) {
    ct.log("Changing " + archivePath + " and modifying its version to " + newVersion)

    // Change version in *Info.plist
    setFieldValueInPlistFile(cp.map['plistBuddyPath'], archivePath + "/" + "Products/Applications/*.app/*Info.plist", "CFBundleShortVersionString", newVersion)

    // Sign again
    ct.runCommand(cp.map['xcodeCodesignPath'] + " --force --sign " + cp.map['signCertHash'] + " " + archivePath)

    ct.log("Version modification complete")
}

def updateVersionFile(newVersion, file) {
    setFieldValueInPlistFile(cp.map['plistBuddyPath'], file, "CFBundleShortVersionString", newVersion)
    ct.dlog("Updated file " + file + " with version " + newVersion)
}


def determineStoreReleaseAction(doUpload, assetsFile) {
    def action = "assemble"
    
    if (doUpload) {
        action = assetsFile == ""?"publishApk":"publish"
    }

    return action
}

def getIncrementedVersionCode() {
    def vc = (ct.getCommandOutput("grep STORE_VERSION_CODE " + cp.map['versionPropertiesPath'] + " | cut -d'=' -f2", ct.generatedUUID, false).replaceAll("\n", "")).toInteger()
    vc += 1
    return vc
}

def getVersionStore(valueMap) {
    return sprintf( '%1$s-%2$s', 
            valueMap['versionShort'], 
            valueMap['branchIncrement'])
}

def getVersionStoreIOS(valueMap) {
    return sprintf('%1$s.%2$s',
            valueMap['versionShort'],
            valueMap['branchIncrement'])
}

return this;
