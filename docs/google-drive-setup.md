# Google Drive setup

dotfiles can sync your saved dotfiles through your own Google Drive. It doesn't come with Google credentials of its
own, so you create a free OAuth client in Google Cloud once and give its file to dotfiles. This takes about ten minutes.

dotfiles only asks for the `drive.file` permission, which lets it see the files it creates and nothing else in your
Drive.

The commands below use the `dotfiles` alias from the [README](../README.md).

## 1. Create a Google Cloud project

1. Open the [Google Cloud console](https://console.cloud.google.com/) and sign in with the Google account whose Drive
   you want to sync to.
2. Open the project picker at the top of the page and choose **New project**.
3. Give it a name, such as `dotfiles`, and choose **Create**.
4. Make sure the new project is selected in the project picker before carrying on.

## 2. Enable the Google Drive API

1. Go to **APIs & Services > Library**.
2. Search for **Google Drive API**, open it and choose **Enable**.

## 3. Set up the consent screen

1. Go to **Google Auth platform > Branding**. If it says the Google Auth platform isn't configured yet, choose
   **Get started**.
2. Under **App information**, enter an app name, such as `dotfiles`, and pick your email as the user support email.
   Choose **Next**.
3. Under **Audience**, choose **External**. **Internal** is only available to Google Workspace organisations. Choose
   **Next**.
4. Under **Contact information**, enter your email address. Choose **Next**.
5. Agree to the Google API Services User Data Policy, then choose **Continue** and **Create**.

You don't need to add any scopes. dotfiles asks for `drive.file` itself when you log in.

## 4. Publish the app

**Don't skip this step.** A new app starts in **Testing**, and Google makes logins to an app in Testing expire after
7 days. Everything works at first, then a week later sync stops and you have to log in again, every week.

1. Go to **Google Auth platform > Audience**.
2. Under **Publishing status**, choose **Publish app**, confirming if asked. The status changes to **In production**,
   and a **Back to testing** button takes its place in case you ever need to undo it.

`drive.file` doesn't need Google's review, so the app is usable as soon as it is published.

If you'd rather leave the app in **Testing**, Google only lets listed test users log in. On the same **Audience**
page, under **Test users** (below the OAuth user cap), choose **+ Add users** and enter your Google account's email
address. This section only appears while the app is in Testing, and logins still expire after 7 days.

## 5. Create a Desktop app client

1. Go to **Google Auth platform > Clients** and choose **Create client**.
2. Set **Application type** to **Desktop app**. Other types, such as **Web application**, don't work with dotfiles.
3. Give it a name (only you see it) and choose **Create**.
4. Download the JSON file from the dialog that appears. **This is the only chance to download it**: Google doesn't let
   you view or download a client secret again once the dialog is closed. Google names the file
   `client_secret_<id>.apps.googleusercontent.com.json`.

This file holds your client secret, so don't share it or commit it anywhere.

If you lose the file, add a new secret to the same client:

1. In **Google Auth platform > Clients**, open the Desktop app client.
2. Under **Client secrets**, choose **Add secret**.
3. Download it straight away with the download button next to the new secret. Once you leave the page it can't be
   downloaded again, and you would have to add another one.

The new file is different from the old one, so dotfiles treats it as a new client (see
[Switching to a different client](#switching-to-a-different-client)).

## 6. Log in

Tell dotfiles to sync with Google Drive, then hand it the downloaded file:

```shell
dotfiles config --set SyncProvider:GDrive
dotfiles login --credentials ~/Downloads/client_secret_<id>.apps.googleusercontent.com.json
```

`--credentials` (`-c`) also accepts a folder, as long as it holds only one `client_secret_*.json` file, so
`dotfiles login -c ~/Downloads` works too.

dotfiles copies the file into place, then opens your browser. Choose your account and allow access. When the browser shows "Received verification
code. You may now close this window.", close the tab and dotfiles finishes the login. If the browser doesn't open,
visit the link dotfiles printed.

The original download is left where it was. Once you're logged in you can delete it.

Check everything is in order with:

```shell
dotfiles status
```

## Placing the file by hand

Instead of `--credentials`, you can copy the file yourself. It must be named `credentials.json` and go in this folder:

| OS      | Folder                                           |
|---------|--------------------------------------------------|
| Linux   | `~/.config/dotfiles/GDrive/`                     |
| macOS   | `~/Library/Application Support/dotfiles/GDrive/` |
| Windows | `%USERPROFILE%\AppData\Local\dotfiles\GDrive\`   |

Then run `dotfiles login`. Using `--credentials` is safer, since it checks the file before it copies it and makes the
folder readable only by you.

## Switching to a different client

This covers a new secret for the same client too. dotfiles won't replace the file while you're logged in, since a
login belongs to the client it was made with, so log out first:

```shell
dotfiles logout
dotfiles login --credentials <new file> --force
```

Without `--force`, dotfiles refuses to replace a different file that's already in place. If you give it the same file
again, it says so and logs in as usual.

## Troubleshooting

- **`Credentials file '...' not found`:** dotfiles has no client file yet. Follow the steps above, or check the file is
  named `credentials.json` if you placed it by hand.
- **`Dotfiles needs desktop client credentials, found web client credentials`:** the client was created as a
  **Web application**. Create a new **Desktop app** client (step 5) and use its file.
- **`Credentials file is not an OAuth client file`** or **`Credentials file has invalid JSON`:** the file isn't the
  client's JSON download. A service account key, for example, won't work. Download the file again from the **Clients**
  list.
- **Google warns "Google hasn't verified this app … an app that's currently being tested" when you log in:** the app
  is still in **Testing**. You can carry on, since it's your own app, but publish it (step 4) or the login expires in
  7 days.
- **You have to log in again every week:** the app is still in **Testing**. Publish it (step 4), then run
  `dotfiles logout` and `dotfiles login`.
- **Google blocks the login, saying the app is only available to testers:** the app is still in **Testing** and your
  account isn't a test user. Publish it, or add your account as a test user (both in step 4).

To remove dotfiles' access to your Drive, run `dotfiles logout`. You can also remove it from the
[Google account permissions page](https://myaccount.google.com/permissions).
