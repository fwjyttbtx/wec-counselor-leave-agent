package com.wisedu.wec.weccounselorleaveagent.exception

/**
 *
 * @author wjfu@wisedu.com
 */
class AccessTokenException : RuntimeException {
    constructor() : super()
    constructor(message: String) : super(message)
}