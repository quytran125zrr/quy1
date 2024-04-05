package com.xxx.zzz.aall.okhttp3ll;

import com.xxx.zzz.aall.okhttp3ll.internalss.httpnn.HttpHeaders;

import java.util.concurrent.TimeUnit;

import com.xxx.zzz.aall.javaxlll.annotationlll.Nullableq;


public final class CacheControlz {
  
  public static final CacheControlz FORCE_NETWORK = new Builder().noCache().build();

  
  public static final CacheControlz FORCE_CACHE = new Builder()
      .onlyIfCached()
      .maxStale(Integer.MAX_VALUE, TimeUnit.SECONDS)
      .build();

  private final boolean noCache;
  private final boolean noStore;
  private final int maxAgeSeconds;
  private final int sMaxAgeSeconds;
  private final boolean isPrivate;
  private final boolean isPublic;
  private final boolean mustRevalidate;
  private final int maxStaleSeconds;
  private final int minFreshSeconds;
  private final boolean onlyIfCached;
  private final boolean noTransform;
  private final boolean immutable;

  @Nullableq
  String headerValue; 

  private CacheControlz(boolean noCache, boolean noStore, int maxAgeSeconds, int sMaxAgeSeconds,
                        boolean isPrivate, boolean isPublic, boolean mustRevalidate, int maxStaleSeconds,
                        int minFreshSeconds, boolean onlyIfCached, boolean noTransform, boolean immutable,
                        @Nullableq String headerValue) {
    this.noCache = noCache;
    this.noStore = noStore;
    this.maxAgeSeconds = maxAgeSeconds;
    this.sMaxAgeSeconds = sMaxAgeSeconds;
    this.isPrivate = isPrivate;
    this.isPublic = isPublic;
    this.mustRevalidate = mustRevalidate;
    this.maxStaleSeconds = maxStaleSeconds;
    this.minFreshSeconds = minFreshSeconds;
    this.onlyIfCached = onlyIfCached;
    this.noTransform = noTransform;
    this.immutable = immutable;
    this.headerValue = headerValue;
  }

  CacheControlz(Builder builder) {
    this.noCache = builder.noCache;
    this.noStore = builder.noStore;
    this.maxAgeSeconds = builder.maxAgeSeconds;
    this.sMaxAgeSeconds = -1;
    this.isPrivate = false;
    this.isPublic = false;
    this.mustRevalidate = false;
    this.maxStaleSeconds = builder.maxStaleSeconds;
    this.minFreshSeconds = builder.minFreshSeconds;
    this.onlyIfCached = builder.onlyIfCached;
    this.noTransform = builder.noTransform;
    this.immutable = builder.immutable;
  }

  
  public boolean noCache() {
    return noCache;
  }

  
  public boolean noStore() {
    return noStore;
  }

  
  public int maxAgeSeconds() {
    return maxAgeSeconds;
  }

  
  public int sMaxAgeSeconds() {
    return sMaxAgeSeconds;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public boolean isPublic() {
    return isPublic;
  }

  public boolean mustRevalidate() {
    return mustRevalidate;
  }

  public int maxStaleSeconds() {
    return maxStaleSeconds;
  }

  public int minFreshSeconds() {
    return minFreshSeconds;
  }

  
  public boolean onlyIfCached() {
    return onlyIfCached;
  }

  public boolean noTransform() {
    return noTransform;
  }

  public boolean immutable() {
    return immutable;
  }

  
  public static CacheControlz parse(Headersza headers) {
    boolean noCache = false;
    boolean noStore = false;
    int maxAgeSeconds = -1;
    int sMaxAgeSeconds = -1;
    boolean isPrivate = false;
    boolean isPublic = false;
    boolean mustRevalidate = false;
    int maxStaleSeconds = -1;
    int minFreshSeconds = -1;
    boolean onlyIfCached = false;
    boolean noTransform = false;
    boolean immutable = false;

    boolean canUseHeaderValue = true;
    String headerValue = null;

    for (int i = 0, size = headers.size(); i < size; i++) {
      String name = headers.name(i);
      String value = headers.value(i);

      if (name.equalsIgnoreCase("Cache-Control")) {
        if (headerValue != null) {
          
          canUseHeaderValue = false;
        } else {
          headerValue = value;
        }
      } else if (name.equalsIgnoreCase("Pragma")) {
        
        canUseHeaderValue = false;
      } else {
        continue;
      }

      int pos = 0;
      while (pos < value.length()) {
        int tokenStart = pos;
        pos = HttpHeaders.skipUntil(value, pos, "=,;");
        String directive = value.substring(tokenStart, pos).trim();
        String parameter;

        if (pos == value.length() || value.charAt(pos) == ',' || value.charAt(pos) == ';') {
          pos++; 
          parameter = null;
        } else {
          pos++; 
          pos = HttpHeaders.skipWhitespace(value, pos);

          
          if (pos < value.length() && value.charAt(pos) == '\"') {
            pos++; 
            int parameterStart = pos;
            pos = HttpHeaders.skipUntil(value, pos, "\"");
            parameter = value.substring(parameterStart, pos);
            pos++; 

            
          } else {
            int parameterStart = pos;
            pos = HttpHeaders.skipUntil(value, pos, ",;");
            parameter = value.substring(parameterStart, pos).trim();
          }
        }

        if ("no-cache".equalsIgnoreCase(directive)) {
          noCache = true;
        } else if ("no-store".equalsIgnoreCase(directive)) {
          noStore = true;
        } else if ("max-age".equalsIgnoreCase(directive)) {
          maxAgeSeconds = HttpHeaders.parseSeconds(parameter, -1);
        } else if ("s-maxage".equalsIgnoreCase(directive)) {
          sMaxAgeSeconds = HttpHeaders.parseSeconds(parameter, -1);
        } else if ("private".equalsIgnoreCase(directive)) {
          isPrivate = true;
        } else if ("public".equalsIgnoreCase(directive)) {
          isPublic = true;
        } else if ("must-revalidate".equalsIgnoreCase(directive)) {
          mustRevalidate = true;
        } else if ("max-stale".equalsIgnoreCase(directive)) {
          maxStaleSeconds = HttpHeaders.parseSeconds(parameter, Integer.MAX_VALUE);
        } else if ("min-fresh".equalsIgnoreCase(directive)) {
          minFreshSeconds = HttpHeaders.parseSeconds(parameter, -1);
        } else if ("only-if-cached".equalsIgnoreCase(directive)) {
          onlyIfCached = true;
        } else if ("no-transform".equalsIgnoreCase(directive)) {
          noTransform = true;
        } else if ("immutable".equalsIgnoreCase(directive)) {
          immutable = true;
        }
      }
    }

    if (!canUseHeaderValue) {
      headerValue = null;
    }
    return new CacheControlz(noCache, noStore, maxAgeSeconds, sMaxAgeSeconds, isPrivate, isPublic,
        mustRevalidate, maxStaleSeconds, minFreshSeconds, onlyIfCached, noTransform, immutable,
        headerValue);
  }

  @Override public String toString() {
    String result = headerValue;
    return result != null ? result : (headerValue = headerValue());
  }

  private String headerValue() {
    StringBuilder result = new StringBuilder();
    if (noCache) result.append("no-cache, ");
    if (noStore) result.append("no-store, ");
    if (maxAgeSeconds != -1) result.append("max-age=").append(maxAgeSeconds).append(", ");
    if (sMaxAgeSeconds != -1) result.append("s-maxage=").append(sMaxAgeSeconds).append(", ");
    if (isPrivate) result.append("private, ");
    if (isPublic) result.append("public, ");
    if (mustRevalidate) result.append("must-revalidate, ");
    if (maxStaleSeconds != -1) result.append("max-stale=").append(maxStaleSeconds).append(", ");
    if (minFreshSeconds != -1) result.append("min-fresh=").append(minFreshSeconds).append(", ");
    if (onlyIfCached) result.append("only-if-cached, ");
    if (noTransform) result.append("no-transform, ");
    if (immutable) result.append("immutable, ");
    if (result.length() == 0) return "";
    result.delete(result.length() - 2, result.length());
    return result.toString();
  }

  
  public static final class Builder {
    boolean noCache;
    boolean noStore;
    int maxAgeSeconds = -1;
    int maxStaleSeconds = -1;
    int minFreshSeconds = -1;
    boolean onlyIfCached;
    boolean noTransform;
    boolean immutable;

    
    public Builder noCache() {
      this.noCache = true;
      return this;
    }

    
    public Builder noStore() {
      this.noStore = true;
      return this;
    }

    
    public Builder maxAge(int maxAge, TimeUnit timeUnit) {
      if (maxAge < 0) throw new IllegalArgumentException("maxAge < 0: " + maxAge);
      long maxAgeSecondsLong = timeUnit.toSeconds(maxAge);
      this.maxAgeSeconds = maxAgeSecondsLong > Integer.MAX_VALUE
          ? Integer.MAX_VALUE
          : (int) maxAgeSecondsLong;
      return this;
    }

    
    public Builder maxStale(int maxStale, TimeUnit timeUnit) {
      if (maxStale < 0) throw new IllegalArgumentException("maxStale < 0: " + maxStale);
      long maxStaleSecondsLong = timeUnit.toSeconds(maxStale);
      this.maxStaleSeconds = maxStaleSecondsLong > Integer.MAX_VALUE
          ? Integer.MAX_VALUE
          : (int) maxStaleSecondsLong;
      return this;
    }

    
    public Builder minFresh(int minFresh, TimeUnit timeUnit) {
      if (minFresh < 0) throw new IllegalArgumentException("minFresh < 0: " + minFresh);
      long minFreshSecondsLong = timeUnit.toSeconds(minFresh);
      this.minFreshSeconds = minFreshSecondsLong > Integer.MAX_VALUE
          ? Integer.MAX_VALUE
          : (int) minFreshSecondsLong;
      return this;
    }

    
    public Builder onlyIfCached() {
      this.onlyIfCached = true;
      return this;
    }

    
    public Builder noTransform() {
      this.noTransform = true;
      return this;
    }

    public Builder immutable() {
      this.immutable = true;
      return this;
    }

    public CacheControlz build() {
      return new CacheControlz(this);
    }
  }
}
