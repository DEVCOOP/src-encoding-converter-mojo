src-encoding-converter-mojo
===========================

Because I have to write it more than once and because I don't want to see ant stuff in my project, 
I share this little mojo. 

   Which convert all sources and resources from an encoding to another.

Example of usage :

    mvn fr.devcoop.mojos:src-encoding-converter:1.0-SNAPSHOT:convert -DsourceEncoding=ISO-8859-1 -DtargetEncoding=UTF-8

