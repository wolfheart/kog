package com.danneu.kog

import com.danneu.kog.batteries.multipart.SavedUpload
import com.danneu.kog.cookies.parse
import com.danneu.kog.json.Decoder
import com.danneu.kog.result.Result
import com.danneu.kog.result.flatMap
import javax.servlet.ReadListener
import javax.servlet.ServletInputStream

class Request(
  val serverPort: Int,
  val serverName: String,
  val remoteAddr: String,
  val href: String,
  var queryString: String?,
  val scheme: String,
  var method: Method,
  val protocol: String,
  override val headers: MutableList<HeaderPair>,
  val type: String?,
  val length: Int?, // Either >= 0 or null
  val charset: String?,
  // TODO: val sslClientCert
  val body: ServletInputStream, // Note: Could just be InputStream if I'm never going to use ServerInputStream methods
  var path: String
) : HasHeaders<Request> {

    val query by lazy {
        formDecode(queryString).mutableCopy()
    }

    val params: MutableMap<String, Any> = mutableMapOf()

    // multipart middleware populates this with name -> file mappings for multipart uploads
    val uploads by lazy {
        mutableMapOf<String, SavedUpload>()
    }

    val negotiate by lazy {
        Negotiator(this)
    }

    val cookies by lazy {
        parse(getHeader(Header.Cookie)).mutableCopy()
    }

    // TODO: At framework level, need to avoid reading stream when it is already being/been consumed or come up with a deliberate gameplan.
    fun <T> json(decoder: Decoder<T>): Result<T, Exception> {
        return Decoder.tryParse(utf8).flatMap { jsonValue -> decoder(jsonValue) }
    }

    // TODO: Handle case where body stream is already read
    val utf8: String by lazy {
        body.readBytes().toString(Charsets.UTF_8)
    }

    fun setMethod(method: Method) = apply { this.method = method }

    override fun toString(): String {
        return listOf(
          "serverPort" to serverPort,
          "serverName" to serverName,
          "remoteAddr" to remoteAddr,
          "href" to href,
          "queryString" to queryString,
          "scheme" to scheme,
          "method" to method.toString(),
          "protocol" to protocol,
          "headers" to headers,
          "type" to type,
          "length" to length,
          "charset" to charset,
          "path" to path
        ).map { pair -> pair.toString() }.joinToString("\n")
    }

    companion object
}


fun Request.Companion.toy(method: Method = Method.Get, path: String = "/", queryString: String = "foo=bar"): Request {
    return Request(
      serverPort = 3000,
      serverName = "name",
      remoteAddr = "1.2.3.4",
      href = path + if (queryString.isNotBlank()) { "?" + queryString } else { "" },
      queryString = queryString,
      scheme = "http",
      method = method,
      protocol = "http",
      headers = mutableListOf(),
      type = null,
      length = 0,
      charset = null,
      body = ToyStream(),
      path = path
    )
}


// Lets us mock a Request with a noop input stream
class ToyStream: ServletInputStream() {
    override fun isReady(): Boolean = true
    override fun isFinished(): Boolean = true
    override fun read(): Int = -1
    override fun setReadListener(readListener: ReadListener?) {}
}
