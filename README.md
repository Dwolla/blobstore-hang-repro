# fs2-hang-repro

When an error occurs after bytes having been piped through `readOutputStream` / `writeOutputStream`, the stream seems to hang. This is a small project designed to show the issue.

```
👀 processing chunk #0 of 10485760 bytes
```

… and then nothing happens.
