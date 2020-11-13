SCIPER=257234

cp -r src $SCIPER
cd $SCIPER
rm -r .settings/ .classpath .gitignore .project target
cd ..
zip -r $SCIPER.zip $SCIPER
rm -r $SCIPER
