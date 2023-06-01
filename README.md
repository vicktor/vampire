![vampire_xs](https://github.com/vicktor/vampire/assets/382114/29897482-3b7f-4144-a540-c460e2b3d794)
# vampire | Delighted to suck your glucoses
###### _Give me back my **fucking data**_


Read glucose from notifications and **share** it with **anyone** through internal WebServer and ContentProvider.
Optionally you can write all information in Google [Health Connect](https://developer.android.com/guide/health-and-fitness/health-connect).

Based on the great [xDrip+](https://github.com/NightscoutFoundation/xDrip) project without which, **vampire** would not exist.

## Configuration

App needs special permission to read your notifications, you can inspect the code for your safety and peace of mind.

Go to Settings -> Accessibility -> Notifications and allow **vampire** to access your notifications, allow all

## Reading data with WebServer

Open http://localhost:5566/glucoses from your app to read last 24 hours of glucoses

Open http://localhost:5566/glucose from your app to read last glucose value

All data are returned in JSON:

```json
{ 
  "glucoseType":"interstitial",
  "glucoseUnits":"mgdl",
  "glucoseValue":140.0,
  "origin":"Dexcom",
  "timestamp":1685461844641,
  "timestampOffset":120,
  "trend":"FLAT"
}
```
## Contributing
Of course, collaboration and pull requests are welcome. 

## Thanks
Again kudos to awesome xDrip+ Team and all people fighting against diabetes. 
