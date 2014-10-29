Create test cases
1. Create your own test cases in the testcases folder.
   (some samples are provided. The output are not validated. Use at your own risk!)
   in - (a) filename
        Use the format <number>-<web service name>.[txt|zip].
        Only bootstrap should ends with the zip extension and is a 
        binary file. 
        e.g. 01-bootstrap.zip
             02-bid-dump.txt 
        (b) Content
        specify the json content you need to send to the web service.
        For bootstrap, it is the data zip file. 
        For other services, it is the JSON content you are sending 
        over in the r parameter. If you are not sending over any values,
        leave the content blank.
   out - The expected JSON output
   yours - Your web service JSON output

Compile
Extract the files to a directory, and type "compile" in the directory.
The class files will be compiled to the classes folder. (You need to create the classes folder!)

Run
type "run <URL>" in the command prompt window (you should be in 
the directory where you have extracted the files). e.g.

D:\checker> run http://2013-g8t8.rhcloud.com/myprefix
Test Case 1 passed
Test Case 2 passed
Total: 2/2