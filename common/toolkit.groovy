// Toolkit

import hudson.model.*
import static groovy.json.JsonOutput.*
import groovy.json.JsonOutput

generatedUUID = getUUID()

def log(msg) {
    echo '[INFO] ' + msg
}

def dlog(msg) {
    // check if debugMode (build variable) is true
    if (_MAP['debugMode'])
        echo '[DEBUG] ' + msg
}

def exceptionToString(e) {
    StringWriter writer = new StringWriter();
    PrintWriter printWriter = new PrintWriter(writer);
    e.printStackTrace(printWriter);
    printWriter.flush();

    return writer.toString();
}


def getCommandOutput(cmd, filename, resultAlwaysTrue) {
    if (resultAlwaysTrue)
        cmd = cmd + ' > ' + filename + ' || true'
    else
        cmd = cmd + ' > ' + filename

    dlog("Running shell command: '" + cmd + "'")
    sh cmd
    def result = readFile file: filename, encoding: 'utf-8'
    sh 'rm ' + filename
    dlog("Command output: '" + result + "'")
    return result
}

def runCommand(cmd) {
    dlog("Running command: '" + cmd + "' on a " + isUnix() ? "UNIX" : "WINDOWS" + " environment")
    if (isUnix()) {
        sh cmd
    } else {
        bat cmd
    }
}


def printMapVars(map) {
    log(prettyPrint(toJson(map)))
}

@NonCPS
def mapToPropString(map) {
    p = ""

    for (e in map) {
        p = p + "${e.key} = ${e.value}\n"
    }

    return p
}


def getUUID() {
    log("Generating random UUID ...")
    return UUID.randomUUID().toString()
}

def setCurrentBuildDescription(description) {
    hudson.model.Hudson.instance.getItem(env.JOB_NAME).getBuildByNumber(Integer.parseInt(env.BUILD_NUMBER)).setDescription(description)
}


def setCurrentBuildDisplayName(name) {
    hudson.model.Hudson.instance.getItem(env.JOB_NAME).getBuildByNumber(Integer.parseInt(env.BUILD_NUMBER)).setDisplayName(name)
}

def getLastCompletedBuildDescription() {
    return hudson.model.Hudson.instance.getItem(env.JOB_NAME).getLastCompletedBuild().getDescription()
}

def getCurrentBuildEnvVars() {
    return hudson.model.Hudson.instance.getItem(env.JOB_NAME).getBuildByNumber(Integer.parseInt(env.BUILD_NUMBER)).getEnvironment()
}

def getLastCompletedBuildEnvVar(varName) {
    return hudson.model.Hudson.instance.getItem(env.JOB_NAME).getLastCompletedBuild().getEnvironment()[varName]
}

def getLastCompletedBuildEnvVars() {
    return hudson.model.Hudson.instance.getItem(env.JOB_NAME).getLastCompletedBuild().getEnvironment()
}


def createDescription(map) {
    def d = "<STRONG>Version:</STRONG> <DLM>" + map['version'] + " (branch: " + map['gitBranch'] + ")</DLM><BR>" +
            "<STRONG>HEAD Rev:</STRONG> <DLM>" + map['revision'] + "</DLM><BR>" +
            "<STRONG>Release State:</STRONG> <DLM>" + map['releaseState'] + "</DLM> <BR>" +
            "<STRONG>Last Snapshot:</STRONG> <DLM>" + map['gitLastSnapshotTag'] + "</DLM> <BR>" +
            "<STRONG>Issues since last Snapshot:</STRONG> <DLM>" + map['gitLogSubjectsSinceLastSnapshot'] + "</DLM> <BR>" +
            "<STRONG>Last Release:</STRONG> <DLM>" + map['gitLastReleaseTag'] + "</DLM> <BR>" +
            "<STRONG>Issues since last Release:</STRONG> <DLM>" + map['gitLogSubjectsSinceLastRelease'] + "</DLM> <BR>" +
            "<STRONG>Release Descriptor:</STRONG> <DLM>" + map['releaseDescriptorFile'] + "</DLM> <BR>"
    return d
}

def getGitLogSubjectsByTag(tag) {
    // Expecting format @TICKET-ID:message
    return getCommandOutput('git log ' + tag + '..HEAD | grep "^    @" | sed -e "s/^    //g"', this.generatedUUID, true).trim()
}

def getGitHEADShortRevision() {
    return getCommandOutput("git log --pretty=format:%h -1", this.generatedUUID, false)
}

def getGitHEADLongRevision() {
    return getCommandOutput("git log --pretty=format:%H -1", this.generatedUUID, false)
}

def getGitCommitIncrementInBranch(branch) {
    /*

    As per http://tgoode.com/2014/06/05/sensible-way-increment-bundle-version-cfbundleversion-xcode/

    Why not make a virtual “revision number” based on the number of commits that exist on a certain branch? Since I base all my work on a ‘development’ branch, 
    I can simply have git count the number of commits on development and use that as my build number. I’m anchoring it to a specific branch so that the revision 
    number is consistent: even if I check out another branch – to work on a new feature, for example – the number always points to the same commit and won’t 
    differ between branches. This of course requires the branch used for counting to be long-lasting, and it’s only really useful if it’s where you distribute 
    most of your builds from.

    */
    def a = (getCommandOutput("git rev-list " + branch + " --count", this.generatedUUID, false).trim()).toInteger() as int
    def b = (getCommandOutput("git rev-list HEAD.." + branch + " --count", this.generatedUUID, false).trim()).toInteger() as int

    return (a - b)
}

def getLatestGitTag() {
    return getCommandOutput('git describe --abbrev=0 --tags', this.generatedUUID, true).trim()
}

def getLatestMatchingGitTag(match) {
    return getCommandOutput("git log --simplify-by-decoration --pretty='format:%d' HEAD | grep 'tag:' | grep '\\" + match + "' | head -n1 | grep -o '[^[:space:]]*\\" + match + "[^[:space:]]*' | sed 's/.\$//'", this.generatedUUID, true).trim()
}

def getLatestReleaseGitTag() {
    dlog("LATEST RELEASE TAG IS: '" + getLatestMatchingGitTag("-REL") + "'")

    return getLatestMatchingGitTag("-REL")
}

def getLatestSnapshotGitTag() {
    def result = getLatestMatchingGitTag("-SNA")

    if (isLatestGitTagRelease()) {
        result = getLatestReleaseGitTag()
    }

    dlog("LATEST SNAPSHOT TAG IS: '" + result + "'")

    return result
}

def isLatestGitTagRelease() {
    def result = false

    if (getLatestGitTag().contains("-REL")) {
        result = true
    }

    return result
}

def getLatestGitTagRevision() {
    latestTag = getLatestGitTag()
    return getCommandOutput('git rev-list -n 1 ' + latestTag, this.generatedUUID, true).trim()
}


def getCodeVersionFromFile(versionFilePath) {
    return ((readFile(versionFilePath)).trim()).replaceAll("\n", "")
}

def getLastCommitterEmail() {
    return getCommandOutput("git show -s --pretty=%ce", this.generatedUUID, false).replaceAll("\n", "");
}

def getLastCommitterName() {
    return getCommandOutput("git show -s --pretty=%cn", this.generatedUUID, false).replaceAll("\n", "");
}

def checkoutSpecificGitTag(tag) {
    return runCommand("git checkout tags/" + tag);
}


def addJIRAComment(issue, comment, identifier) {
    log("Adding JIRA comment: Ticket=" + issue + " Comment=" + comment + " with identifier=" + identifier)

    sh _MAP['pyjiPath'] + 'pyji.py -a comment -U "' + _MAP['jiraURL'] +
            '" -u "' + _MAP['jiraUsername'] +
            '" -p "' + _MAP['jiraPassword'] +
            '" -k "' + issue +
            '" -c "' + comment +
            '" -D "' +
            '" -i "' + identifier + '"'
}


def transitionJIRAIssue(issue, identifier) {
    log("Transitioning JIRA ticket: Ticket=" + issue)
    sh _MAP['pyjiPath'] + 'pyji.py -a autotransition -U "' + _MAP['jiraURL'] +
            '" -u "' + _MAP['jiraUsername'] +
            '" -p "' + _MAP['jiraPassword'] +
            '" -k "' + issue +
            '" -D "' +
            '" -i "' + identifier + '"'
}


def createJIRAIssue(project, subject, description, type, assignee, issueMap) {
    def issueCSV = createIssueCSV(issueMap, _MAP['jiraURL'])

    //dlog("Issue CSV: " + issueCSV)

    sh _MAP['pyjiPath'] + 'pyji.py -a createissue -U "' + _MAP['jiraURL'] +
            '" -u "' + _MAP['jiraUsername'] +
            '" -p "' + _MAP['jiraPassword'] +
            '" -D "' +
            '" -k "' + project +
            '" -s "' + subject +
            '" -d "' + description +
            '" -t "' + type +
            '" -A "' + assignee +
            '" -L "' + issueCSV + '"'
}

def updateJIRAIssuesFromLog(gitLogSubjects, identifier) {
    /*

    The following block of code should be uncommented only when pipelines
    sort out the serialization problem that occurs when trying to execute a shell command (addcomment/transitionissue) with a map variable

    Until then I will keep the proper solution commented out and resort to a very ugly solution below. */

    //for (String line : gitLogSubjects.split("\n")) {
    //    // Expecting @KEY-ID:comment \n @KEY-ID:comment2 etc
    //    String key = gitLogSubjects.replaceFirst( /.*(?<=@)(.+?):.*/, '$1' )
    //    String comment = gitLogSubjects.replaceFirst( /.*(?<=:).+.*/, '$0' )

    //    addJIRAComment(key, comment, identifier)
    //    transitionJIRAIssue(comment, identifier)
    //}

    String s = gitLogSubjects

    if (s == null) {
        log("Commit log is empty. Skipping JIRA update.")
        return
    }

    dlog("Ready to parse git log '" + s + "'")

    // Hack
    s = s + "\n"

    while (s.indexOf("@") != -1) {
        key = ""
        comment = ""
        ticket = ""

        ticket = s.substring(s.indexOf("@"), s.indexOf("\n"))

        dlog("TICKET entry found '" + ticket + "'")

        if (ticket.indexOf(":") != -1) {
            key = ticket.substring(ticket.indexOf("@") + 1, ticket.indexOf(":")).trim()
            comment = ticket.substring(ticket.indexOf(":") + 1, ticket.length()).trim()
        } else {
            key = ticket.substring(ticket.indexOf("@") + 1, ticket.length()).trim()
            comment = ""
        }

        dlog("KEY: '" + key + "'")
        dlog("COMMENT: '" + comment + "'")

        try {

            addJIRAComment(key, comment, identifier)
            transitionJIRAIssue(key, identifier)
        } catch (e) {
            log("An error occured when trying to update ticket with KEY " + key)
        }

        // Strip ticket entry and proceed with the rest if exist
        if (s.length() > 1) {
            s = s.substring(s.indexOf("\n") + 1, s.length())
        } else {
            break
        }

    }

}

def jiraIssuesToMap(gitLogSubjects) {

    def issueMap = [:]

    String s = gitLogSubjects

    if (s == "") {
        log("Commit log is empty. Skipping JIRA issues to dict.")
        return
    }

    dlog("Ready to parse git log '" + s + "'")

    // Hack
    s = s + "\n"

    while (s.indexOf("@") != -1) {
        key = ""
        comment = ""
        ticket = ""

        ticket = s.substring(s.indexOf("@"), s.indexOf("\n"))

        dlog("TICKET entry found '" + ticket + "'")

        if (ticket.indexOf(":") != -1) {
            key = ticket.substring(ticket.indexOf("@") + 1, ticket.indexOf(":")).trim()
            comment = ticket.substring(ticket.indexOf(":") + 1, ticket.length()).trim()
        } else {
            key = ticket.substring(ticket.indexOf("@") + 1, ticket.length()).trim()
            comment = ""
        }

        dlog("KEY: '" + key + "'")
        dlog("COMMENT: '" + comment + "'")

        issueMap.put(key, comment)

        // Strip ticket entry and proceed with the rest if exist
        if (s.length() > 1) {
            s = s.substring(s.indexOf("\n") + 1, s.length())
        } else {
            break
        }

    }


    return issueMap
}

@NonCPS
def createIssueCSV(issueMap, jiraURL) {
    def result = ""
    def filterURL = jiraURL + "/issues/?jql=issue%20in%20("
    for (e in issueMap) {
        result = result + e.key + "|" + jiraURL + "/browse/" + e.key + ","
        filterURL = filterURL + e.key + "%2C"
    }

    result = result + "Filter|" + filterURL.substring(0, filterURL.length() - 3) + ")"

    return result
}

def askTwice(inputID, question, submitter) {
    input id: inputID, message: question, submitter: submitter
    input "Are you sure ?"
}


def incrementVersionByOne(currentVersion) {
    dlog("Current Version is now : " + currentVersion)
    def vList = currentVersion.split("\\.")
    vList[vList.length - 1] = (vList.last()).toInteger() + 1
    def newVersion = vList.join(".")
    dlog("New Version is : " + newVersion)
    return newVersion
}

def updateVersionFile(newVersion, file) {
    runCommand('echo "' + newVersion + '" > ' + file)
    dlog("Updated file " + file + " with version " + newVersion)
}

def gitCommitPush(file, message, branch) {
    // Checkout - this is a workaround because the HEAD becomes detached when checked out for some reason
    // if this causes issues you can try 'git push origin HEAD:master' instead of checkout 
    runCommand('git checkout ' + branch)
    // Commit
    runCommand('git commit ' + file + ' -m "' + message + '"')
    // Pull
    runCommand('git pull origin ' + branch + ' --tags')
    // Push
    runCommand('git push origin ' + branch + ' --tags')
}

def gitTag(version) {
    runCommand('git tag ' + version + " -f")
    runCommand('git push origin ' + version + " -f")
    dlog("Version tagging complete for " + version)
}

def determineSnapshotTarget(env) {
    def t = _MAP['targetTestSnapshot']
    if (env.equalsIgnoreCase("LIVE")) {
        t = _MAP['targetLiveSnapshot']
    }
    return t
}

def determineReleaseTarget(env) {
    def t = _MAP['targetTestRelease']
    if (env.equalsIgnoreCase("LIVE")) {
        t = _MAP['targetLiveRelease']
    }
    return t
}

def prepareDistributionNotesFile(releaseState, gitLogSubjectsSinceLastSnapshot, gitLogSubjectsSinceLastRelease, version, branch) {

    def notes = releaseState + " Notes for " + version + " (branch: " + branch + ") \n\n"

    if (gitLogSubjectsSinceLastSnapshot) {
        notes = notes + "Since last snapshot (or release - whichever was the latest): \n\n" +
                gitLogSubjectsSinceLastSnapshot +
                " \n\n"
    }

    if (gitLogSubjectsSinceLastRelease) {
        notes = notes + "Since last release: \n\n" + gitLogSubjectsSinceLastRelease
    }

    return notes
}

def createFabricDistributionNotesFile(releaseState, version, notes) {
    if (notes.length() > 16384) {
        log(releaseState + " Notes are larger than 16384 characters which is the Fabric limit. Printing them below but sending compact notes.")
        log(notes)

        notes = releaseState + " Notes for " + version + " were too large to fit here. Please request them from EXUS using refid: " + version
    }

    createDistributionNotesFile(releaseState, notes)
}

def createDistributionNotesFile(releaseState, notes) {
    dlog("Writing " + releaseState + " Notes to disk ...")
    writeFile file: getDistributionNotesFilename(releaseState), text: notes
}

def getDistributionNotesFilename(releaseState) {
    return releaseState.toLowerCase() + '_notes.txt'
}

def logS(url, channel, title, titleLink, text, fallback, color, fields) {
    // URL format should follow 'https://hooks.slack.com/services/xxxxxxx/yyyyyyyy/zzzzzzzzzz'

    def attachments = [['title'     : title,
                        'title_link': titleLink,
                        'text'      : text,
                        'fallback'  : fallback,
                        'color'     : color,
                        'fields'    : fields]]

    def payload = JsonOutput.toJson([channel    : channel,
                                     username   : "Jenkins",
                                     icon_emoji : ":jenkins:",
                                     mrkdwn     : "true",
                                     attachments: attachments])

    dlog(prettyPrint(payload))
    runCommand("curl -X POST --data-urlencode \'payload=${payload}\' ${url}")
}

def logSSimple(url, channel, text) {
    // URL format should follow 'https://hooks.slack.com/services/xxxxxxx/yyyyyyyy/zzzzzzzzzz'
    def payload = JsonOutput.toJson([text      : text,
                                     channel   : channel,
                                     username  : "Jenkins",
                                     icon_emoji: ":jenkins:"])
    runCommand("curl -X POST --data-urlencode \'payload=${payload}\' ${url}")
}


def getBinaryFilename(valueMap) {
    return sprintf('%1$s-%2$s-%3$s%4$s%5$s-%6$s-%7$s',
            valueMap['appName'],
            valueMap['versionShort'],
            valueMap['branchIncrement'],
            valueMap['oSName'].take(1),
            valueMap['targetEnvironment'].take(1),
            valueMap['revisionShort'],
            valueMap['releaseState'].take(3))
}

def getVersion(valueMap) {
    return sprintf('%1$s-%2$s%3$s%4$s-%5$s',
            valueMap['versionShort'],
            valueMap['branchIncrement'],
            valueMap['oSName'].take(1),
            valueMap['targetEnvironment'].take(1),
            valueMap['releaseState'].take(3))
}

def isStartedByGitPush() {
    def result = false
    def cause = currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause)

    if (cause == null) {
        dlog("Build started automatically after a push was made to the repository")
        result = true
    }

    return result
}

def uploadToFTP(server, port, username, password, file) {

}


return this;
