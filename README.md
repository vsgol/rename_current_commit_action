# Rename Current Commit (IntelliJ Plugin)

Simple plugin for IntelliJ IDEA that allows you to rename the latest Git commit via Git menu

---

## Features

- Adds an action **"Rename Current Commit"** to the `Git` menu.
- Executes `git commit --amend` in the background.

---

## Instalation

This project is written in **Kotlin** and uses **Gradle** as its build system. Follow the steps below to set up and run the plugin:

### Prerequisites

**1. IntelliJ IDEA 2024.1.7**

**2. JDK 17 or higher** installed and configured.

### Build the Project

Use the Gradle to build the project:

```bash
./gradlew buildPlugin
```

Plagin will be in [build/distributions](build/distributions)

## TODO
- I still haven't figured out how to refresh the git window to update the commit in the tree 
- If there are no commits, there will most likely be some kind of error. 
