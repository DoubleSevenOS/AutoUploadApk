package com.yyhu.auto

class AutoPluginExtension {
    String filePath
    //蒲公英，apiKey
    String apiKey
    String webHook
    String appName
    String appVersion
    String appCode
    //0,钉钉，1微信
    String msgType
    String author
    String qrImgUrl

    @Override
    String toString() {
        return "filePath:" + filePath + ",apiKey:" + apiKe + ",verionName:" + verionName + ",webHook:" + webHook
    }
}
