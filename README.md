![vampire_xs](https://github.com/vicktor/vampire/assets/382114/29897482-3b7f-4144-a540-c460e2b3d794)
# vampire | Delighted to suck your glucoses
###### _Give me back my **fucking data**_

Read glucose from notifications and **share** it with **anyone** through internal WebServer and ContentProvider.
Optionally you can write all information in Google [Health Connect](https://developer.android.com/guide/health-and-fitness/health-connect).

Based on the great [xDrip+](https://github.com/NightscoutFoundation/xDrip) project without which, **vampire** would not exist.

## Screenshots

![vampire](https://github.com/vicktor/vampire/assets/382114/62048346-2e58-4d82-bf9f-1365a646d314)
![health](https://github.com/vicktor/vampire/assets/382114/1d60fde0-abae-41a5-ac75-86bc41354d2c)
![glucosa](https://github.com/vicktor/vampire/assets/382114/fbd1f990-2c8b-4ce4-b09f-317394f8b552)
 

## Configuration

App needs special permission to read your notifications, you can inspect the code for your safety and peace of mind.

Go to Settings -> Accessibility -> Notifications and allow **vampire** to access your notifications, allow all
Go to Settings -> Batery ->  Select **vampire**, Battery Saving and select **No Restrictions**


## CGM supported

From xDrip+
```
    coOptedPackages.add("com.dexcom.g6")
    coOptedPackages.add("com.dexcom.g6.region1.mmol")
    coOptedPackages.add("com.dexcom.g6.region3.mgdl")
    coOptedPackages.add("com.dexcom.dexcomone")
    coOptedPackages.add("com.dexcom.g7")
    coOptedPackages.add("com.camdiab.fx_alert.mmoll")
    coOptedPackages.add("com.camdiab.fx_alert.mgdl")
    coOptedPackages.add("com.camdiab.fx_alert.hx.mmoll")
    coOptedPackages.add("com.camdiab.fx_alert.hx.mgdl")
    coOptedPackages.add("com.medtronic.diabetes.guardian")
    coOptedPackages.add("com.medtronic.diabetes.minimedmobile.eu")
    coOptedPackages.add("com.medtronic.diabetes.minimedmobile.us")
```

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
## Bugs
Sometimes the service stops and does not continue to collect data. 
If anyone knows how to fix this, open an issue or make a pull request.

## Contributing
Of course, collaborations and pull requests are welcome. 

## Credits
Icon -> [icon-icons.com](https://icon-icons.com/icon/vampire/2710)

WebServer -> [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd)

## Thanks
Again kudos to awesome xDrip+ Team and all people fighting against diabetes. 
