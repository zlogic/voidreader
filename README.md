# Void Reader
Quick &amp; dirty RSS-to-email tool

This project is a simple, quick&amp;dirty application to download RSS feeds and send them via SMTP or upload via IMAP to your mailbox.

Runs on Google App Engine.

Your mailbox will provide an interface to view feed and (optionally) view the source contents by downloading the feed links and descriptions.

Can work as a simple and permanent replacement for Google Reader and works with all email clients, even on not-so-popular mobile operating systems.

Installation
-----
1. Create a Google App Engine account and a new project
2. Create a new project, write down its Project ID (*project_id*)
3. Get a copy of Void Reader (I recomend to fork this repository)
4. Set your *project_id* in /pom.xml's <voidreader.appengine.projectid/> property: `<voidreader.appengine.projectid>*project_id*</voidreader.appengine.projectid>`
5. Build the project (`mvn clean install`)
6. Deploy to App  by going into `voidreader-webapp` and running `mvn appengine:update`

Configuration
-----
1. Open https://*project_id*.appspot.com/settings/
2. Submit the form with your settings ("PDF enable" doesn't work on App Engine, **do not check it**)
3. You can view the current settings by opening https://*project_id*.appspot.com/admin/settings

This project does rely on authentication, only the project admin is allowed to change or view the settings. No login required, just make sure you're signed into Google as the same use who created the project.

It's very likely you're going to exceed the App Engine daily quota for sending emails (100 mails per day) the first time. Don't worry, you will get the emails on the next day. If you have too many RSS subscriptions, consider running multiple instances of this app and distributing feeds between them.

Miscellaneous
-----

The project history has a version which can run in standalone mode: check feeds, send updates via SMTP or upload via IMAP to your mailbox and quit. This is most suitable for running from cron. You'll need to configure an external mail service to send emails.
This version can also download the linked articles and turn them into PDF attachments. PDF are ugly (most CSS is ignored, Javascript is not supported), but allow to download items on a fast unlimited connection for offline viewing later.

This version can be found in the [standalone](../../tree/standalone) tag.
