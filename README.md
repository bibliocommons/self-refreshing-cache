# SelfRefreshingCache

## Overview

An in-memory cache implementation with built-in automatic refresh on a configurable period backed by a threadpool. SelfRefreshingCache simplifies the semantics of 
 the usual get-check-then-set style. Additionally, supports the following features:
  
 * Supports default values to seed cache
 * Configurable fail on initial load
 * Staggered refresh delays to minimize clustering of reloads
  
Used extensively in production at [BiblioCommons](http://bibliocommons.com) since late 2010. 

## Compatibility

Compatible with Java 6 and above.
  
## Usage
  
  * Provide a key satisfying the rules for HashMap keys (`equals()` and `hashcode()`)

## Contributors

See [contributors.md](contributors.md)
