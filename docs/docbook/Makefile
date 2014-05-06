html:
	db2html --html --output html-adminguide swamp-adminguide.xml;
	cp -r images-adminguide html-adminguide;
	rm -rf html-adminguide/images-adminguide/.svn
	db2html --html --output html-installguide swamp-installguide.xml
	cp -r images-installguide html-installguide;
	rm -rf html-installguide/images-installguide/.svn
	db2html --html --output html-develguide swamp-develguide.xml
	cp -r images-develguide html-develguide;
	rm -rf html-develguide/images-develguide/.svn


pdf:
	mkdir -p pdf-adminguide; 
	cp -r images-adminguide pdf-adminguide
	rm -rf pdf-adminguide/images-adminguide/.svn
	db2html --pdf --output pdf-adminguide  swamp-adminguide.xml;
	mkdir -p pdf-installguide;
	cp -r images-installguide pdf-installguide
	rm -rf pdf-installguide/images-installguide/.svn
	db2html --pdf --output pdf-installguide swamp-installguide.xml
	mkdir -p pdf-develguide;
	cp -r images-develguide pdf-develguide
	rm -rf pdf-develguide/images-develguide/.svn
	db2html --pdf --output pdf-develguide swamp-develguide.xml

clean:
	rm -rf pdf-* html-* CATALOG.local *.tex *~;
