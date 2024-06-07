# 北京大学智能电子系统设计与实践课程项目：BlackEye
## 项目概述
本项目为北京大学智能电子系统设计与实践课程项目，本仓库为安卓用户端应用部分。BlackEye旨在复刻《RainbowSix:Siege》游戏中Valkyrie干员的技能：“黑眼”摄像头。
在游戏中，“黑眼”摄像头是一个信息获取道具（[技能演示视频](https://markdown.com.cn)）；在我们复刻的过程中，希望让它具有更生活化的功能，最终设定为家庭场景下的安防摄像头应用。<br>
安卓用户端应用采用Kotlin，API Level = 31开发，该应用只能在支持Andorid 12的设备上运行。Configuration language为Kotlin DSL。<br>
安卓用户端应用功能包括：
+ 登录/注册用户
+ 在主页面中查看BlackEye采集的视频
+ 使用摇杆控制BlackEye旋转，以采集不同方向的视频
+ 在页面的右侧使用3D模型可视化BlackEye的姿态
+ 在页面的左侧抽屉式菜单栏中，有以下选项：
   + 休眠/唤醒BlackEye
   + 查看用户信息和登出
   + 关闭应用
+ 对画面中的物体进行视觉识别

## 使用说明
### 和服务器连接
在使用时，请将`app/src/main/java/com/example/blackeye/network/RetrofitClinet.kt`中的<br>
```
private const val BASE_URL = "http://your_ip_address"
```
修改为您自己的公网服务器IP地址。由于我们在开发阶段没有申请证书，因此传输协议没有采用https，您可以根据自己的需要修改。<br>
为了与服务器建立WebSocket连接，请您将`app/src/main/java/com/example/blackeye/WebSocketClient.kt`中的<br>
```
private val serverUri = URI("ws://your_ip_address/user")
```
修改为您自己的公网服务器IP地址。

### 弃用的功能
您在使用时，可以看到视频界面右上角有一个电池图标，并且MainActivity中也有对其更新的逻辑，但由于我们并未设计对电池电量的采样电路，该功能暂时被弃用，您可以根据需要修改代码以启用。

