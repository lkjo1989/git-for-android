package com.gitforandroid.domain.parser

sealed class GitCommand {
    data class Init(val path: String? = null) : GitCommand()
    data class Clone(val url: String, val path: String? = null) : GitCommand()
    data class Status(val porcelain: Boolean = false) : GitCommand()
    data class Add(val files: List<String>) : GitCommand()
    data class Commit(val message: String, val all: Boolean = false) : GitCommand()
    data class Push(val remote: String? = null, val branch: String? = null) : GitCommand()
    data class Pull(val remote: String? = null, val branch: String? = null) : GitCommand()
    data class Fetch(val remote: String? = null) : GitCommand()
    data class Log(
        val maxCount: Int = 50,
        val oneline: Boolean = false,
        val graph: Boolean = false
    ) : GitCommand()
    data class Branch(
        val listAll: Boolean = false,
        val delete: String? = null,
        val create: String? = null,
        val verbose: Boolean = false
    ) : GitCommand()
    data class Checkout(val branch: String, val createNew: Boolean = false) : GitCommand()
    data class Merge(val branch: String) : GitCommand()
    data class Diff(val staged: Boolean = false, val file: String? = null) : GitCommand()
    data class Stash(val subCommand: String? = null) : GitCommand() // null = push, "pop", "list"
    data class Config(val key: String, val value: String? = null) : GitCommand()
    object Remote : GitCommand()
    data class Unknown(val raw: String) : GitCommand()
}
