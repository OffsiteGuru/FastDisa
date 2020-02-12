# FastDisa

## Synopsis
FastDisa is being created to duplicate the features of Google Voice for mobile users with the help of Asterisk.

**Call Routing**

It contacts the Asterisk server over the internet and configures the call routing. When your phone dials, it dials your Asterisk server which follows the route.

If internet is not available, the app will still dial though using DTMF tones to route the call.

**SMS Support**

Maybe we'll get this integrated in the future. For now I'm using [Twilio Hosted SMS](https://www.twilio.com/docs/phone-numbers/hosted-numbers) and the app from [textable.co](https://www.textable.co/docs/twilio-setup-guide/) which is working just fine.

See [The Project Brief](/ProjectBrief.md) for more info.
