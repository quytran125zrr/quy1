
package com.xxx.zzz.aall.okhttp3ll;

import java.io.IOException;


public enum Protocolza {

  HTTP_1_0("http/1.0"),


  HTTP_1_1("http/1.1"),


  SPDY_3("spdy/3.1"),


  HTTP_2("h2");

  private final String protocol;

  Protocolza(String protocol) {
    this.protocol = protocol;
  }


  public static Protocolza get(String protocol) throws IOException {
    
    if (protocol.equals(HTTP_1_0.protocol)) return HTTP_1_0;
    if (protocol.equals(HTTP_1_1.protocol)) return HTTP_1_1;
    if (protocol.equals(HTTP_2.protocol)) return HTTP_2;
    if (protocol.equals(SPDY_3.protocol)) return SPDY_3;
    throw new IOException("Unexpected protocol: " + protocol);
  }


  @Override public String toString() {
    return protocol;
  }
}
