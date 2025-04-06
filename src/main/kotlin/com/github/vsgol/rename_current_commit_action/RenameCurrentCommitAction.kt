package com.github.vsgol.rename_current_commit_action

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitCommandResult
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager

class RenameCurrentCommitAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val repo = GitRepositoryManager.getInstance(project).repositories.firstOrNull()
        if (repo == null) {
            notifyError(project, "No Git repository found.")
            return
        }

        object : Task.Backgroundable(project, "Loading last commit message") {
            override fun run(indicator: ProgressIndicator) {
                val currentMessage = getLastCommitMessage(project, repo)

                ApplicationManager.getApplication().invokeLater {
                    if (project.isDisposed) return@invokeLater

                    val newMessage = Messages.showInputDialog(
                        project,
                        "Enter a new commit message:",
                        "Rename Current Commit",
                        null,
                        currentMessage,
                        null
                    )

                    if (newMessage == null) return@invokeLater

                    if (newMessage.isBlank()) {
                        Messages.showErrorDialog(
                            project,
                            "Commit message must not be empty.",
                            "Rename Commit"
                        )
                        return@invokeLater
                    }

                    object : Backgroundable(project, "Renaming last commit") {
                        override fun run(indicator: ProgressIndicator) {
                            if (indicator.isCanceled) return

                            val result = runAmendCommit(project, repo, newMessage)

                            ApplicationManager.getApplication().invokeLater {
                                if (result.success()) {
                                    notifySuccess(project)
                                } else {
                                    notifyError(project, result.errorOutput.joinToString("\n"))
                                }
                            }
                        }
                    }.queue()
                }
            }
        }.queue()
    }



    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    private fun getLastCommitMessage(project: Project, repo: GitRepository): String {
        val handler = GitLineHandler(project, repo.root, GitCommand.LOG)
        handler.setSilent(true)
        handler.addParameters("-1", "--pretty=%B")

        val result = Git.getInstance().runCommand(handler)
        return if (result.success()) result.output.joinToString("\n").trim() else ""
    }

    private fun runAmendCommit(project: Project, repo: GitRepository, newMessage: String): GitCommandResult {
        val handler = GitLineHandler(project, repo.root, GitCommand.COMMIT)
        handler.setSilent(false)
        handler.addParameters("--amend", "-m", newMessage)
        return Git.getInstance().runCommand(handler)
    }

    private fun notifySuccess(project: Project) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Git Rename Commit Notifications")
            .createNotification(
                "Rename commit",
                "Commit amended successfully",
                NotificationType.INFORMATION
            )
            .notify(project)
    }

    private fun notifyError(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Git Rename Commit Notifications")
            .createNotification(
                "Rename commit failed",
                message,
                NotificationType.ERROR
            )
            .notify(project)
    }
}
