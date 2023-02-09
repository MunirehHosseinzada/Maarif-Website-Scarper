package com.ustadmobile.downloader

import org.jsoup.Jsoup
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

fun main(args: Array<String>){

    val url = "https://web.archive.org/web/20221001233742/https://maarif.af/"
    val subjectLinks = mutableListOf<Subject>()
    var csvString = ""

    try {
        val document = Jsoup.connect(url).sslSocketFactory(SSLHelperKotlin.socketFactory()).get()

        val gradesLinks = document.select("div.row > a").map {
            Grade(
                gradeLink = "https://web.archive.org" + it.attr("href"),
                gradeName = it.text()
            )
        }

        gradesLinks.map { grade ->
            val gradeDocument = Jsoup.connect(grade.gradeLink).sslSocketFactory(SSLHelperKotlin.socketFactory()).get()

            gradeDocument.select("div.col-md-4 > a").forEach {
                subjectLinks.add(
                    Subject(
                        subjectLink = "https://web.archive.org" + it.attr("href"),
                        subjectPath = "دری," + grade.gradeName + "," + it.text()
                    )
                )
            }

            println("********** ${grade.gradeName}")
        }

        println(subjectLinks)

        subjectLinks.map { subject ->
            val subjectDocument = Jsoup.connect(subject.subjectLink).sslSocketFactory(SSLHelperKotlin.socketFactory()).get()

            subjectDocument.select("td > a").forEach{
                if (it.attr("role") == "button"){
                    val lesson = it.text()
                    it.parent()?.select("a")?.forEach {  innerA ->
                        if (innerA.attr("role") != "button"){
                            val lessonName = innerA.text()
                            innerA.parent()?.select("iframe")?.forEach { iframe ->
                                val link = iframe.attr("src").split("https://")
                                if (link.count() == 3){
                                    File("youtubeLinks.csv").printWriter().use {  file ->
                                        csvString += "${subject.subjectPath},$lesson,$lessonName,https://${link[2]}\n\r"
                                        file.println(csvString)

                                        file.flush()
                                    }
                                }
                            }
                        }
                    }
                }
            }
            println("^^^^^^^^^^^^^${subject.subjectPath}")
        }

    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

class SSLHelperKotlin {

    companion object {

        @JvmStatic
        fun socketFactory(): SSLSocketFactory {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
                    override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
                }
            )

            return try {
                val sslContext: SSLContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, SecureRandom())
                sslContext.socketFactory
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException("Failed to create a SSL socket factory", e)
            } catch (e: KeyManagementException) {
                throw RuntimeException("Failed to create a SSL socket factory", e)
            }
        }
    }
}