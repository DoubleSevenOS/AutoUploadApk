package com.yyhu.auto


import groovy.json.JsonSlurper
import groovy.json.StringEscapeUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

class AutoBuild implements Plugin<Project> {

    String appVersion = ""
    String appCode = ""
    //0,钉钉，1微信
    String msgType = "1"
    String appName = "";
    String author = "DoubleSeven";
    String qrImgUrl = "";

    @Override
    void apply(Project project) {
        // 向extension container保存para参数
        project.extensions.create("para", AutoPluginExtension)
        // 向project对象添加hello任务
        project.task('autoBuild', type: MyTask) {
            dependsOn("assembleRelease")
            // 设置greeting参数
            doLast {
                author = project.para.author
                msgType = project.para.msgType
                appVersion = project.para.appVersion
                appCode = project.para.appCode
                appName = project.para.appName
                qrImgUrl = project.para.qrImgUrl
                println "********************************************************************************"
                println "***apiKey  :${project.para.apiKey}"
                println "***webHook :${project.para.webHook}"
                println "****author :${author}"
                println "***msgType :${msgType}"
                println "appVersion :${appVersion}"
                println "***appCode :${appCode}"
                println "***appName :${appName}"
                println "**qrImgUrl :${qrImgUrl}"
                println "********************************************************************************"
                uploadApk(project.para.filePath, project.para.apiKey, "", project.para.webHook);
            }
        }
    }

    void uploadApk(String path, String apiKey, String pwd, String webHook) {
        //查找上传的apk文件，这里需要换成自己apk路径
        long startTime = System.currentTimeMillis();
        println "***************开始上传蒲公英***************"

        def apkDir = new File(path)
        if (!apkDir.exists()) {
            throw new RuntimeException("apk output path not exists!")
        }

        def apk = null
        for (int i = apkDir.listFiles().length - 1; i >= 0; i--) {
            File file = apkDir.listFiles()[i]
            if (file.name.endsWith(".apk")) {
                apk = file
                break
            }
        }
        if (apk == null) {
            throw new RuntimeException("apk file not exists!")
        }

        println "***************文件上传中***************"

        def twoHyphens = "--"
        def boundary = "*********"
        def end = "\r\n"

        //模拟表单上传 multipart/form-data
        def conn = new URL("https://www.pgyer.com/apiv2/app/upload").openConnection()
        conn.setRequestMethod('POST')
        conn.setRequestProperty("Connection", "Keep-Alive")
        conn.setRequestProperty("Charset", "UTF-8")
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary)
        conn.setDoInput(true)
        conn.setDoOutput(true)
        //添加参数：_api_key
        def sb = new StringBuilder()
        sb.append(twoHyphens).append(boundary).append(end)
        sb.append("Content-Disposition: form-data; name=_api_key")
        sb.append(end).append(end)
        sb.append(apiKey).append(end)

        //添加参数：buildUpdateDescription 更新日志，取值gradle.properties中的 BUILD_NOTES
        sb.append(twoHyphens).append(boundary).append(end)
        sb.append("Content-Disposition: form-data; name=buildUpdateDescription")
        sb.append(end).append(end)
        sb.append("自动化打包").append(end)
        //安装方式，密码安装
        sb.append(twoHyphens).append(boundary).append(end)
        sb.append("Content-Disposition: form-data; name=buildInstallType")
        sb.append(end).append(end)
        if (pwd != null && pwd.length() > 0) {
            println pwd

            sb.append("2").append(end)
            //安装密码
            sb.append(twoHyphens).append(boundary).append(end)
            sb.append("Content-Disposition: form-data; name=buildPassword")
            sb.append(end).append(end)
            sb.append(pwd).append(end)
        } else {
            sb.append("1").append(end)
        }

        //添加参数file: 需要上传的apk文件
        sb.append(twoHyphens).append(boundary).append(end)
        sb.append("Content-Disposition: form-data; name=file;filename=").append(apk.getName())
        sb.append(end).append(end)
        def dos = new DataOutputStream(conn.getOutputStream())
        dos.writeBytes(sb.toString())
        dos.flush()
        sb.delete(0, sb.length())

        def fis = new FileInputStream(apk)
        byte[] bf = new byte[8192]
        int len
        while ((len = fis.read(bf)) != -1) {
            dos.write(bf, 0, len)
        }
        sb.append(end)
        sb.append(twoHyphens).append(boundary).append(end)
        dos.writeBytes(sb.toString())

        dos.flush()
        fis.close()
        dos.close()
        conn.connect()

        def text = conn.getContent().text
        def resp = new JsonSlurper().parseText(text)

        println text

        long endTime = System.currentTimeMillis();

        println "***************上传蒲公英成功，耗时：${(endTime - startTime) / 1000}秒***************"
        if (resp.code != 0) {
            throw new RuntimeException(resp.message)
        }
        println resp
        if ("1" == msgType) {
            sendMsgToWeiXin(resp.data, webHook)
        } else {
            sendMsgToDing(resp.data, webHook)
        }

    }

    def sendMsgToDing(def data, String webHook) {
        println "*************** 准备发送钉钉消息 ***************"
        def conn = new URL(webHook).openConnection()
        conn.setRequestMethod('POST')
        conn.setRequestProperty("Connection", "Keep-Alive")
        conn.setRequestProperty("Content-type", "application/json;charset=UTF-8")
        conn.setConnectTimeout(30000)
        conn.setReadTimeout(30000)
        conn.setDoInput(true)
        conn.setDoOutput(true)
        def dos = new DataOutputStream(conn.getOutputStream())

        def downloadUrl = "https://www.pgyer.com/" + data.buildShortcutUrl
        def qrCodeUrl = "![](" + data.buildQRCodeURL + ")"
        def detailLink = "[项目地址](${downloadUrl})"
        def _title = "蒲公英机器人"
        def _content = new StringBuffer()
        _content.append("\n\n###Android 测试包构建成功")
        _content.append("\n\n构建版本:${appVersion}")
        _content.append("\n\n下载地址:" + downloadUrl)
        _content.append("\n\n" + qrCodeUrl)
        _content.append("\n\n构建用户:yyHu")
        _content.append("\n\n构建时间:" + getCurData())
        _content.append("\n\n查看详情:" + detailLink)
        def json = new groovy.json.JsonBuilder()
        json {
            msgtype "markdown"
            markdown {
                title _title
                text _content.toString()
            }
            at {
                atMobiles([])
                isAtAll false
            }
        }

        println(json)
        dos.writeBytes(json.toString())
        def input = new BufferedReader(new InputStreamReader(conn.getInputStream()))
        String line = ""
        String result = ""
        while ((line = input.readLine()) != null) {
            result += line
        }
        dos.flush()
        dos.close()
        input.close()
        conn.connect()
        println(_content.toString())
        println(result)
        println("*************** 钉钉消息已发送 ***************")

    }

    def sendMsgToWeiXin(def data, String webHook) {
        println "*************** 准备发送企业微信消息消息***************"
        def conn = new URL(webHook).openConnection()
        conn.setRequestMethod('POST')
        conn.setRequestProperty("Connection", "Keep-Alive")
        conn.setRequestProperty("Content-type", "application/json;charset=UTF-8")
        conn.setRequestProperty("Accept-Charset", "UTF-8")
        conn.setRequestProperty("contentType", "UTF-8")
        conn.setConnectTimeout(30000)
        conn.setReadTimeout(30000)
        conn.setDoInput(true)
        conn.setDoOutput(true)
        def dos = new DataOutputStream(conn.getOutputStream())

        def downloadUrl = "https://www.pgyer.com/" + data.buildShortcutUrl
        def qrCodeUrl = data.buildQRCodeURL

        def titleName = ""

        if (appName != null && !appName.isEmpty()) {
            titleName = appName
        }

        def authorKey = "构建者"

        def qrUrl = qrCodeUrl
        if (qrImgUrl != null && qrImgUrl != "null" && !qrImgUrl.isEmpty()) {
            qrUrl = qrImgUrl
        }

        def json = new groovy.json.JsonBuilder()
        def cuTime = getCurData()
        json {
            msgtype "template_card"
            template_card {
                card_type "news_notice"
                source {
                    icon_url "https://wework.qpic.cn/wwpic/252813_jOfDHtcISzuodLa_1629280209/0"
                    desc titleName
                    desc_color "0"
                }
                main_title {
                    title "欢迎使用 AutoUploadApk"
                    desc "DoubleSevenOs"
                }
                card_image {
                    url qrUrl
                    aspect_ratio "1.5"
                }
                vertical_content_list([
                        {
                            title "Android自动化打包"
                            desc ""
                        }
                ])
                horizontal_content_list([
                        {
                            keyname authorKey
                            value author
                        },
                        {
                            keyname "VersionName"
                            value appVersion
                        },
                        {
                            keyname "VersionCode"
                            value appCode
                        },
                        {
                            keyname "构建时间"
                            value cuTime
                        },
                ])
                jump_list([
                        {
                            type "1"
                            title "下载链接"
                            url downloadUrl
                        }
                ])
                card_action {
                    type "1"
                    url downloadUrl
                }
            }
        }
        println(json)
        def str = StringEscapeUtils.unescapeJava(json.toString()).trim()
        println(str)
        dos.write(str.getBytes("utf-8"))
        def input = new BufferedReader(new InputStreamReader(conn.getInputStream()))
        String line = ""
        String result = ""
        while ((line = input.readLine()) != null) {
            result += line
        }
        dos.flush()
        dos.close()
        input.close()
        conn.connect()
        println(result)
        println("*************** 微信消息已发送 ***************")

    }


    String getCurData() {
        return new Date().format('yyyy-MM-dd hh:mm:ss');
    }
}
