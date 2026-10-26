# Dotfiles

A simple command-line tool to manage, store, and restore your configuration files.

## Features

- **Centralized Storage**: Save your dotfiles in a single internal storage.
- **Easy Restoration**: Quickly restore files to their original or custom locations.
- **Safety First**: Confirmation prompts before overwriting files.
- **Path Abstraction**: Handles `~` home directory references automatically.
- **Logging**: Keeps track of operations for debugging.

## Installation

### Prerequisites

- Java 22 or higher.
- Maven (for building).

### Build

```bash
mvn clean package
```

The executable JAR will be located in the `target` directory.

## Usage

Run the tool using the generated JAR:

```bash
java -jar target/dotfiles.jar [COMMAND] [OPTIONS]
```

**Hint:** You can set an alias to avoid typing down the entire command each time

```bash
alias dotfiles="java -jar target/dotfiles.jar"
```

And then simply run

```bash
dotfiles [COMMAND] [OPTIONS]
```

### Available Commands

- `save -k <key> -f <file>`: Save a new config file.
- `list`: List all currently saved files.
- `view <key>`: View details and content of a saved file.
- `place <key> [-p <path>] [-f]`: Restore a file to its location.
    - `-p, --path`: Override the original path. Use `#` at the end of the path to keep the original filename in that
      directory.
    - `-f`: Create missing parent directories.
- `update <key> [-k <newKey>] [-p <newPath>] [-d]`: Update a saved entry.
    - `-d`: By default, the `update` command reads the saved file so this flag tells the app not to read the saved file
- `remove <key>`: Remove an entry from storage (does not delete the physical file).
- `logs [-r]`: Locate the logs file.
    - `-r`: Locate the file and then read it
- `purge [-l] [-s]`: Delete application data (logs and/or saved files).
    - `-l`: Exclude the logs file from the purge
    - `-s`: Exclude the save file from the purge

## Data Storage

By default, dotfiles and logs are stored in your user's Documents folder under a `dotfiles` directory.
