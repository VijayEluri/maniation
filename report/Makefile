.SUFFIXES = .tex .bib .aux .bbl .dvi .ps .pdf

all:	wordcount diss-final.pdf techreport.ps
	@echo ''
	@echo -n 'Word count: '
	@perl $(HOME)/tools/latex2text.pl diss.tex | wc -w

wordcount:
	sed -i~ -e "s/[0-9]*%WORDCOUNT%/"`perl ~/tools/latex2text.pl diss.tex | wc -w | tr -d '\n'`"%WORDCOUNT%/" diss.tex

diss-final.pdf:	diss-ps.pdf diss.pdf
	pdftk S=diss-ps.pdf D=diss.pdf cat D1-18 S19 D20-31 S32 D33-40 S41 D42 S43 D44-56 S57-58 D59 S60 D61-62 S63 D64-end output - | pdftk - update_info pdf.info output diss-final.pdf

diss.pdf:	*.tex
	pdflatex diss

diss-ps.pdf:	diss.ps
	gs -sDEVICE=pdfwrite -dPDFSETTINGS=/printer -sOutputFile=diss-ps.pdf -dBATCH -dNOPAUSE diss.ps

diss.ps:	diss.dvi
	dvips -Ppdf diss.dvi

diss.dvi:	diss.bbl
	latex diss
	latex diss

diss.bbl:	diss.bib diss.aux
	bibtex diss

diss.aux:	*.tex
	latex diss

techreport.pdf:	techreport.ps
	gs -sDEVICE=pdfwrite -dPDFSETTINGS=/prepress -dBATCH -dNOPAUSE -dSAFER -sOutputFile=techreport.pdf techreport.ps

techreport.ps:	techreport.dvi
	dvips -Ppdf -G0 techreport.dvi

techreport.dvi:	techreport.bbl
	latex techreport
	latex techreport

techreport.bbl:	diss.bib techreport.aux
	bibtex techreport

techreport.aux:	*.tex
	latex techreport

clean:
	rm -f *.{log,aux,bbl,blg,dvi,ps}

veryclean:	clean
	rm -f diss.pdf
