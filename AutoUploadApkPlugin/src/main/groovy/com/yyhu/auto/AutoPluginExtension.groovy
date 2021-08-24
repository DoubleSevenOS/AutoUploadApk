package com.yyhu.auto

class AutoPluginExtension {
    String filePath
    //蒲公英，apiKey
    String apiKey
    //钉钉webHook
    String webHook
    String verionName

    @Override
    String toString() {
        return "filePath:" + filePath+",apiKey:" + apiKe+",verionName:" + verionName+",webHook:" + webHook
    }
}
