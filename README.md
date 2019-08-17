# HTTPServiceAttacker-Java

Development builds: https://ci.ishland.com/job/ThievesKiller/job/HTTPServiceAttacker-Java/job/master/

Chinese document: https://github.com/thieveskiller/HTTPServiceAttacker-Java/blob/master/README-CN.md

# Usage
- Download the build or build yourself
- Launch the build using ``` java -jar yourbuild.jar ```
- Edit the configuration file ``` config.yml ```
- Launch the build again
- Enjoy

# Configuration
- showExceptions: Whether we show exception details
- targets: A array where stores the targets
- - addr: The remote url
- - threads: The count of threads to use
- - mode: Using ``` GET ``` or ``` POST ``` to do requests.
- - data: When using ``` POST ```, the request body
- - referer: The header ``` Referer ``` value to set
