# fs2-hang-repro

When an error occurs after bytes having been piped through `readOutputStream` / `writeOutputStream`, the stream seems to hang. This is a small project designed to show the issue.

```
2022-06-24 13:06:09,298 | log_level=INFO  | logger=repro.Boot | log_thread=io-compute-2 | log_message='👀 processing chunk #0 of 10485760 bytes'
2022-06-24 13:06:09,304 | log_level=INFO  | logger=repro.Boot | log_thread=io-compute-10 | log_message='👀 processing chunk #1 of 10485760 bytes'
2022-06-24 13:06:09,308 | log_level=INFO  | logger=repro.Boot | log_thread=io-compute-5 | log_message='👀 processing chunk #2 of 10485760 bytes'
```

… and then nothing happens.
