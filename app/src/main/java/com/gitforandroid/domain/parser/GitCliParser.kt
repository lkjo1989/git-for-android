package com.gitforandroid.domain.parser

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitCliParser @Inject constructor() {

    fun parse(input: String): GitCommand {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return GitCommand.Unknown("")

        val tokens = tokenize(trimmed)
        if (tokens.isEmpty() || tokens[0] != "git") {
            return GitCommand.Unknown(trimmed)
        }

        if (tokens.size < 2) return GitCommand.Unknown(trimmed)

        val subcommand = tokens[1]
        val args = tokens.drop(2)

        return when (subcommand) {
            "init" -> parseInit(args)
            "clone" -> parseClone(args)
            "status" -> parseStatus(args)
            "add" -> parseAdd(args)
            "commit" -> parseCommit(args)
            "push" -> parsePush(args)
            "pull" -> parsePull(args)
            "fetch" -> parseFetch(args)
            "log" -> parseLog(args)
            "branch" -> parseBranch(args)
            "checkout", "co" -> parseCheckout(args)
            "merge" -> parseMerge(args)
            "diff" -> parseDiff(args)
            "stash" -> parseStash(args)
            "remote" -> GitCommand.Remote
            else -> GitCommand.Unknown(trimmed)
        }
    }

    private fun tokenize(input: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var inSingleQuote = false
        var inDoubleQuote = false

        for (ch in input) {
            when {
                ch == '\'' && !inDoubleQuote -> inSingleQuote = !inSingleQuote
                ch == '"' && !inSingleQuote -> inDoubleQuote = !inDoubleQuote
                ch.isWhitespace() && !inSingleQuote && !inDoubleQuote -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) tokens.add(current.toString())
        return tokens
    }

    private fun parseInit(args: List<String>): GitCommand {
        var path: String? = null
        val iter = args.iterator()
        while (iter.hasNext()) {
            when (val arg = iter.next()) {
                "--bare" -> { /* ignore for now */ }
                else -> if (!arg.startsWith("-")) path = arg
            }
        }
        return GitCommand.Init(path)
    }

    private fun parseClone(args: List<String>): GitCommand {
        var url: String? = null
        var path: String? = null
        val iter = args.iterator()
        while (iter.hasNext()) {
            when (val arg = iter.next()) {
                else -> {
                    if (!arg.startsWith("-")) {
                        if (url == null) url = arg else path = arg
                    }
                }
            }
        }
        return if (url != null) GitCommand.Clone(url.trimEnd('/'), path)
        else GitCommand.Unknown("git clone (no url)")
    }

    private fun parseStatus(args: List<String>): GitCommand {
        val porcelain = args.any { it == "--porcelain" || it == "-s" }
        return GitCommand.Status(porcelain)
    }

    private fun parseAdd(args: List<String>): GitCommand {
        val files = args.filter { !it.startsWith("-") }
        return GitCommand.Add(files.ifEmpty { listOf(".") })
    }

    private fun parseCommit(args: List<String>): GitCommand {
        var message = ""
        val all = args.any { it == "-a" || it == "--all" ||
            (it.startsWith("-") && !it.startsWith("--") && it.length > 2 && it.contains('a')) }
        var i = 0
        while (i < args.size) {
            when {
                args[i] == "-m" && i + 1 < args.size -> {
                    message = args[i + 1]
                    i += 2
                    continue
                }
                args[i].startsWith("-m") && args[i].length > 2 -> {
                    message = args[i].removePrefix("-m")
                }
                // Combined short flags containing -m (e.g., -am, -ma)
                !args[i].startsWith("--") && args[i].startsWith("-") &&
                    args[i].contains('m') && args[i].length > 2 -> {
                    val mIndex = args[i].indexOf('m')
                    if (mIndex + 1 < args[i].length) {
                        message = args[i].substring(mIndex + 1)
                    } else if (i + 1 < args.size) {
                        message = args[i + 1]
                        i++
                    }
                }
            }
            i++
        }
        return GitCommand.Commit(message, all)
    }

    private fun parsePush(args: List<String>): GitCommand {
        var remote: String? = null
        var branch: String? = null
        for (arg in args) {
            if (!arg.startsWith("-")) {
                if (remote == null) remote = arg else branch = arg
            }
        }
        return GitCommand.Push(remote, branch)
    }

    private fun parsePull(args: List<String>): GitCommand {
        var remote: String? = null
        var branch: String? = null
        for (arg in args) {
            if (!arg.startsWith("-")) {
                if (remote == null) remote = arg else branch = arg
            }
        }
        return GitCommand.Pull(remote, branch)
    }

    private fun parseFetch(args: List<String>): GitCommand {
        var remote: String? = null
        for (arg in args) {
            if (!arg.startsWith("-")) {
                remote = arg
                break
            }
        }
        return GitCommand.Fetch(remote)
    }

    private fun parseLog(args: List<String>): GitCommand {
        var maxCount = 50
        val oneline = args.any { it == "--oneline" }
        val graph = args.any { it == "--graph" }
        var i = 0
        while (i < args.size) {
            when {
                args[i] == "-n" && i + 1 < args.size -> {
                    maxCount = args[i + 1].toIntOrNull() ?: 50
                    i += 2
                    continue
                }
                args[i].startsWith("-n") && args[i].length > 2 -> {
                    maxCount = args[i].removePrefix("-n").toIntOrNull() ?: 50
                }
            }
            i++
        }
        return GitCommand.Log(maxCount, oneline, graph)
    }

    private fun parseBranch(args: List<String>): GitCommand {
        val listAll = args.any { it == "-a" || it == "--all" }
        val verbose = args.any { it == "-v" || it == "--verbose" }
        var delete: String? = null
        var create: String? = null
        var i = 0
        while (i < args.size) {
            when {
                args[i] == "-d" || args[i] == "--delete" -> {
                    if (i + 1 < args.size) delete = args[i + 1]
                    i += 2
                    continue
                }
                args[i] == "-D" -> {
                    if (i + 1 < args.size) delete = args[i + 1]
                    i += 2
                    continue
                }
                !args[i].startsWith("-") -> {
                    create = args[i]
                }
            }
            i++
        }
        return GitCommand.Branch(listAll, delete, create, verbose)
    }

    private fun parseCheckout(args: List<String>): GitCommand {
        val createNew = args.any { it == "-b" }
        var branch: String? = null
        for (arg in args) {
            if (!arg.startsWith("-")) {
                branch = arg
                break
            }
        }
        return if (branch != null) GitCommand.Checkout(branch, createNew)
        else GitCommand.Unknown("git checkout (no branch)")
    }

    private fun parseMerge(args: List<String>): GitCommand {
        var branch: String? = null
        for (arg in args) {
            if (!arg.startsWith("-")) {
                branch = arg
                break
            }
        }
        return if (branch != null) GitCommand.Merge(branch)
        else GitCommand.Unknown("git merge (no branch)")
    }

    private fun parseDiff(args: List<String>): GitCommand {
        val staged = args.any { it == "--staged" || it == "--cached" }
        var file: String? = null
        for (arg in args) {
            if (!arg.startsWith("-")) {
                file = arg
                break
            }
        }
        return GitCommand.Diff(staged, file)
    }

    private fun parseStash(args: List<String>): GitCommand {
        val subCommand = args.firstOrNull { !it.startsWith("-") }
        return GitCommand.Stash(subCommand)
    }
}
