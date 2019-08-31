objects = puzzle.o boardADT.o

puzzle: $(objects)
	9024 dcc -o puzzle $(objects) -lm
puzzle.o: boardADT.h puzzle.c
boardADT.o: boardADT.h boardADT.c

.PHONY:clean
clean:
	rm -f puzzle $(objects)

