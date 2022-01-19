# AutoUploadApk
项目gradle
...
 dependencies {
      classpath 'com.android.tools.build:gradle:4.1.2'
      classpath 'com.github.DoubleSevenOS:AutoUploadApk:1.0.2'
 }
...


app gradle配置

apply plugin: 'autoBuild'
task uploadApk {
    dependsOn("autoBuild")
    para {
        //构建者名称
        author="yangyanghu"
        //发送消息的类型（0，钉钉。1：企业微信）
        msgType="1"    
        //APP名称
        appName="LoveMedia"
        appVersion="1.1.0"
        appCode="111"
        //蒲公英平台ApiKey
        apiKey = '3e64121ee3bcea38a6a0b8133945fcca'
        //打包路径，默认/app/build/outputs/apk/release
        filePath = './app/build/outputs/apk/release'
        //webhook地址 ，钉钉机器人webhook最好走，关键字文字类型配置
        webHook = 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=74357d3b-46e0-4ab7-a308-ace65d92ca73'
        //由于企业微信最小图片宽高比是1.3 ，蒲公英返回的是1：1的图片。微信不支持，需要手动设置图片地址宽高比1.5的比例。钉钉不需要设置
        qrImgUrl = 'https://s3.bmp.ovh/imgs/2022/01/0c0b4730463ac954.png'
    }
}
