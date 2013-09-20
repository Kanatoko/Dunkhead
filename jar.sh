#!/bin/sh

mkdir tmp
cd bin/
jar -cmf ../jar/MANIFEST.MF ../jar/dunkhead.jar net/
cd ../tmp/
jar -x < ../h2/h2-1.3.173.jar
jar -uf ../jar/dunkhead.jar org/
cd ..
rm -rf tmp

