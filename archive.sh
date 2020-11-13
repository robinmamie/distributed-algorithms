SCIPER=257234

cd src
./cleanup
cd ..
cp -r src $SCIPER
cd $SCIPER
rm -r .settings/ .classpath .gitignore .project target
cd ..
zip -r $SCIPER.zip $SCIPER
rm -r $SCIPER
