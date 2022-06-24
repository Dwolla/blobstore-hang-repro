# blobstore-hang-repro

When an error occurs uploading bytes to S3 after having been piped through `readOutputStream` / `writeOutputStream`, the stream seems to hang. This is a small project designed to show the issue.

### Without a working S3 path
If you don't specify an S3 path, the example generates a random bucket name, so S3 is likely to return a 404 Not Found.
This results in the SDK throwing an exception and triggering the hang. The output should look something like this:

```log4j
🙋 using a randomly generated S3 path (s3://6cd4877b-d6e5-4c0f-9c3b-2b6c9fa994ce/random.gpg), so expect a 404 from S3. This should still trigger the hang.
2022-06-24 12:33:32,298 | log_level=INFO  | logger=repro.Boot | log_thread=io-compute-11 | log_message='👀 processing chunk #0 of 10485760 bytes'
2022-06-24 12:33:32,305 | log_level=INFO  | logger=repro.Boot | log_thread=io-compute-10 | log_message='👀 processing chunk #1 of 10485760 bytes'
2022-06-24 12:33:32,309 | log_level=INFO  | logger=repro.Boot | log_thread=io-compute-3 | log_message='👀 processing chunk #2 of 10485760 bytes'
2022-06-24 12:33:32,373 | log_level=DEBUG | logger=software.amazon.awssdk.request | log_thread=io-compute-8 | log_message='Sending Request: DefaultSdkHttpFullRequest(httpMethod=POST, protocol=https, host=6cd4877b-d6e5-4c0f-9c3b-2b6c9fa994ce.s3.us-west-2.amazonaws.com, port=443, encodedPath=/random.gpg, headers=[amz-sdk-invocation-id, Content-Length, Content-Type, User-Agent], queryParameters=[uploads])'
2022-06-24 12:33:32,918 | log_level=DEBUG | logger=software.amazon.awssdk.requestId | log_thread=aws-java-sdk-NettyEventLoop-1-2 | log_message='Received failed response: 404, Request ID: N305E94THR52VZR2, Extended Request ID: d3gq+yKt/5LoXuvLw/09jxbP0RI39F1nNnJzEm0H+BZ9+TxeRF+K/Rwb+auHLXd3XG/DBlbfag4='
2022-06-24 12:33:32,918 | log_level=DEBUG | logger=software.amazon.awssdk.request | log_thread=aws-java-sdk-NettyEventLoop-1-2 | log_message='Received failed response: 404, Request ID: N305E94THR52VZR2, Extended Request ID: d3gq+yKt/5LoXuvLw/09jxbP0RI39F1nNnJzEm0H+BZ9+TxeRF+K/Rwb+auHLXd3XG/DBlbfag4='
2022-06-24 12:33:37,925 | log_level=DEBUG | logger=software.amazon.awssdk.http.nio.netty.internal.IdleConnectionReaperHandler | log_thread=aws-java-sdk-NettyEventLoop-1-2 | log_message='[Channel: [id: 0x41a04a7e, L:/10.10.20.149:54795 - R:6cd4877b-d6e5-4c0f-9c3b-2b6c9fa994ce.s3.us-west-2.amazonaws.com/52.92.195.82:443]] Closing unused connection (41a04a7e) because it has been idle for longer than 5000 milliseconds.'
```

### With a writable S3 path
  
If you do specify an S3 path to which you have write access, it will probably still eventually throw an exception and trigger the hang.
You'll see a "Failed to make request" error message from the S3 SDK, and then most likely a successful `DELETE` request as it cleans up the
partial upload. Eventually the `IdleConnectionReaperHandler` will print a message about closing unused connection(s):

```log4j
2022-06-24 12:33:15,478 | log_level=INFO  | logger=repro.Boot | log_thread=io-compute-2 | log_message='👀 processing chunk #0 of 10485760 bytes'
2022-06-24 12:33:15,491 | log_level=INFO  | logger=repro.Boot | log_thread=io-compute-3 | log_message='👀 processing chunk #1 of 10485760 bytes'
2022-06-24 12:33:15,496 | log_level=INFO  | logger=repro.Boot | log_thread=io-compute-2 | log_message='👀 processing chunk #2 of 10485760 bytes'
2022-06-24 12:33:15,560 | log_level=DEBUG | logger=software.amazon.awssdk.request | log_thread=io-compute-9 | log_message='Sending Request: DefaultSdkHttpFullRequest(httpMethod=POST, protocol=https, host={bucket}.s3.us-west-2.amazonaws.com, port=443, encodedPath=/random.gpg, headers=[amz-sdk-invocation-id, Content-Length, Content-Type, User-Agent], queryParameters=[uploads])'
2022-06-24 12:33:16,125 | log_level=DEBUG | logger=software.amazon.awssdk.requestId | log_thread=aws-java-sdk-NettyEventLoop-1-2 | log_message='Received successful response: 200, Request ID: BJ7NP92Z05FXQ38A, Extended Request ID: T1A4+mJnvQGcM4nQGTx/dieKytnxxyzjdS+CTmrmctN+1lNbpUrv1K8L+B7wzLaiBugpJPKOUUk='
2022-06-24 12:33:16,125 | log_level=DEBUG | logger=software.amazon.awssdk.request | log_thread=aws-java-sdk-NettyEventLoop-1-2 | log_message='Received successful response: 200, Request ID: BJ7NP92Z05FXQ38A, Extended Request ID: T1A4+mJnvQGcM4nQGTx/dieKytnxxyzjdS+CTmrmctN+1lNbpUrv1K8L+B7wzLaiBugpJPKOUUk='
2022-06-24 12:33:16,404 | log_level=INFO  | logger=repro.Boot | log_thread=io-compute-7 | log_message='👀 processing chunk #3 of 10485760 bytes'
… snip a bunch of chunks …
2022-06-24 12:33:16,605 | log_level=INFO  | logger=repro.Boot | log_thread=io-compute-5 | log_message='👀 processing chunk #52 of 10485760 bytes'
2022-06-24 12:33:17,000 | log_level=DEBUG | logger=software.amazon.awssdk.request | log_thread=io-compute-2 | log_message='Sending Request: DefaultSdkHttpFullRequest(httpMethod=PUT, protocol=https, host={bucket}.s3.us-west-2.amazonaws.com, port=443, encodedPath=/random.gpg, headers=[amz-sdk-invocation-id, Content-Length, Content-Type, User-Agent], queryParameters=[partNumber, uploadId])'
2022-06-24 12:33:47,166 | log_level=DEBUG | logger=software.amazon.awssdk.http.nio.netty.internal.ResponseHandler | log_thread=aws-java-sdk-NettyEventLoop-1-2 | log_message='[Channel: [id: 0xbde6993b, L:/10.10.20.149:54790 - R:{bucket}.s3.us-west-2.amazonaws.com/52.218.168.49:443]] Exception processing request: DefaultSdkHttpFullRequest(httpMethod=PUT, protocol=https, host={bucket}.s3.us-west-2.amazonaws.com, port=443, encodedPath=/random.gpg, headers=[amz-sdk-invocation-id, amz-sdk-request, Authorization, Content-Length, Content-Type, Host, User-Agent, x-amz-content-sha256, X-Amz-Date], queryParameters=[partNumber, uploadId])'
io.netty.handler.timeout.WriteTimeoutException: null
2022-06-24 12:33:47,192 | log_level=DEBUG | logger=software.amazon.awssdk.request | log_thread=io-compute-10 | log_message='Sending Request: DefaultSdkHttpFullRequest(httpMethod=PUT, protocol=https, host={bucket}.s3.us-west-2.amazonaws.com, port=443, encodedPath=/random.gpg, headers=[amz-sdk-invocation-id, Content-Length, Content-Type, User-Agent], queryParameters=[partNumber, uploadId])'
2022-06-24 12:33:47,221 | log_level=DEBUG | logger=software.amazon.awssdk.http.nio.netty.internal.NettyRequestExecutor | log_thread=aws-java-sdk-NettyEventLoop-1-2 | log_message='[Channel: [id: 0xbde6993b, L:/10.10.20.149:54790 ! R:{bucket}.s3.us-west-2.amazonaws.com/52.218.168.49:443]] Failed to make request to https://{bucket}.s3.us-west-2.amazonaws.com/random.gpg?partNumber=1&uploadId=…'
io.netty.channel.StacklessClosedChannelException: null
at io.netty.channel.AbstractChannel$AbstractUnsafe.write(Object, ChannelPromise)(Unknown Source)
2022-06-24 12:33:47,565 | log_level=DEBUG | logger=software.amazon.awssdk.requestId | log_thread=aws-java-sdk-NettyEventLoop-1-13 | log_message='Received successful response: 200, Request ID: GJ72GP9PEY1K0A3X, Extended Request ID: f50W8Z2yP4BX9T6aUy2Ks1KK8a1ewVMbKLxcHkEFEzGFcV2QXUfguhwPJoZis5G8cqKGFVdrTtg='
2022-06-24 12:33:47,566 | log_level=DEBUG | logger=software.amazon.awssdk.request | log_thread=aws-java-sdk-NettyEventLoop-1-13 | log_message='Received successful response: 200, Request ID: GJ72GP9PEY1K0A3X, Extended Request ID: f50W8Z2yP4BX9T6aUy2Ks1KK8a1ewVMbKLxcHkEFEzGFcV2QXUfguhwPJoZis5G8cqKGFVdrTtg='
2022-06-24 12:33:47,571 | log_level=DEBUG | logger=software.amazon.awssdk.request | log_thread=io-compute-1 | log_message='Sending Request: DefaultSdkHttpFullRequest(httpMethod=DELETE, protocol=https, host={bucket}.s3.us-west-2.amazonaws.com, port=443, encodedPath=/random.gpg, headers=[amz-sdk-invocation-id, User-Agent], queryParameters=[uploadId])'
2022-06-24 12:33:47,677 | log_level=DEBUG | logger=software.amazon.awssdk.requestId | log_thread=aws-java-sdk-NettyEventLoop-1-13 | log_message='Received successful response: 204, Request ID: GJ7CHW3X72E30MM5, Extended Request ID: cBXtWDqPe61K3Trb+/n7O+hOt3q3k+Teua5Die5eof1JWjE1UlDfljKg4ax1rlGTeX3MYLI4gUA='
2022-06-24 12:33:47,677 | log_level=DEBUG | logger=software.amazon.awssdk.request | log_thread=aws-java-sdk-NettyEventLoop-1-13 | log_message='Received successful response: 204, Request ID: GJ7CHW3X72E30MM5, Extended Request ID: cBXtWDqPe61K3Trb+/n7O+hOt3q3k+Teua5Die5eof1JWjE1UlDfljKg4ax1rlGTeX3MYLI4gUA='
2022-06-24 12:33:52,682 | log_level=DEBUG | logger=software.amazon.awssdk.http.nio.netty.internal.IdleConnectionReaperHandler | log_thread=aws-java-sdk-NettyEventLoop-1-13 | log_message='[Channel: [id: 0xd4fc7db7, L:/10.10.20.149:54796 - R:{bucket}.s3.us-west-2.amazonaws.com/52.92.195.82:443]] Closing unused connection (d4fc7db7) because it has been idle for longer than 5000 milliseconds.'
```
