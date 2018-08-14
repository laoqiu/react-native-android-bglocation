# react-native-android-bglocation
> 以前使用react-native-android-location获取android位置信息，但原来那个包功能实在简单，早已被我改得面目全非，
> 最近要求锁屏后一直上传位置信息，索性加上headless功能后重新发了一个包
> 如果react-native使用0.55及以下版本，需要修改包下的build.gradle文件，使用build.gradle.v0.55中的内容，否则会编译失败

### 安装

#### yarn下载包
```bash
yarn add react-native-android-bglocation
```

### 引用配置

* In `android/settings.gradle`

```gradle
...
include ':react-native-android-bglocation', ':app'
project(':react-native-android-bglocation').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-android-bglocation')
```

* In `android/app/build.gradle`

```gradle
...
dependencies {
    ...
    compile project(':react-native-android-bglocation')
}
```

* In `android/app/src/main/java/com/yourappname/MainApplication.java`

```java
import com.laoqiu.bglocation.RNALocation;  // <--- import

...
    @Override
    protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
          new RNALocation(), // react-native-android-bglocation
          new MainReactPackage()
      );
    }
...
```

#### 添加权限
```xml
// file: android/app/src/main/AndroidManifest.xml

<uses-permission android:name="android.permission.ACCESS_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

## DeviceEventEmitter 非后台情况下正常监听定位信息
```javascript
...
export default class App extends Component<Props> {
  componentDidMount() {
    DeviceEventEmitter.addListener('updateLocation', (e) => {
      console.log('from frontend ->', e)
    });
    RNALocation.LocationListener()
  }
  render() {
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>Welcome to React Native!</Text>
        <Text style={styles.instructions}>To get started, edit App.js</Text>
        <Text style={styles.instructions}>{instructions}</Text>
      </View>
    );
  }
}
```

## In index.js 注册后台服务
```javascript
...
AppRegistry.registerComponent(appName, () => App);
AppRegistry.registerHeadlessTask('BackgroundLocationTask', () => require('./BackgroundLocationTask'));
```

## BackgroundLocationTask.js 接受后台服务传出来的位置信息
```javascript
module.exports = async (location) => {
  console.log("from backgound ->", location);
  // fetch data to server...
}
```