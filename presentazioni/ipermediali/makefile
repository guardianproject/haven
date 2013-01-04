all: Presentazione.tex
	mkdir -p build
	-pdflatex -interaction=nonstopmode -output-directory=build -interaction=nonstopmode Presentazione.tex
	-pdflatex -interaction=nonstopmode -output-directory=build -interaction=nonstopmode Presentazione.tex
	@-make clean

one: Presentazione.tex
	mkdir -p build
	-pdflatex -interaction=nonstopmode -output-directory=build -interaction=nonstopmode Presentazione.tex
	@-make clean

clean :
	rm build/*.aux
	rm build/*.log
	rm build/*.nav
	rm build/*.out
	rm build/*.snm
	rm build/*.toc
