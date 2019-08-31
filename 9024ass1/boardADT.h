//
// Created by SONG WEI z5198433 on 2019/6/15.
//

#ifndef BOARDADT_H
#define BOARDADT_H
#include <stdbool.h>

/*
we use a struct to describe the board,it's members
include a pointer as the container of configuration
and an int to record the number of all numbers the
integers in the sequence
*/

typedef struct board *pboard;

// functions:Create Board and Initiate
// argument:the void
// return:pointer of Board
pboard CreateAndInit();

// functions:get input from stdin
// 		assignment
// argument:void
// return:pboard
pboard GetConfig(pboard B);

// functions: judge if input is valid
// argument: pboard
// return: if valid return 1 else return 0
int isValid(pboard start, pboard goal);

// functions:judge if size is same
// argument:pboard
// return:if not return false,or true
bool isSameSize(pboard start, pboard goal);

// functions: judge if 'b' exits or not
// argument:pboard
// return: if not return false, or true
bool Findblank(pboard start, pboard goal);

// functions: exit 1 - (N-1)
// argument:pboard
// return: if all in return false or true
bool AllNumbers(pboard start, pboard goal);

// functions: judge if the input is a board
// argument: pboard
// return: if it is return true or return false
bool isBoard(pboard B);

// functions: calculate disorder of board
// argument:pboard
// return: the disorder number
int disorder(pboard B);

// functions: display start and goal to the screen
// argument: pboard
// return: success or failure
int display(pboard start, pboard goal);

// functions: show result
// argument: pboard
// return void
void ShowResult(pboard start, pboard goal);

// functions:calculate the exponent of a number
// argument: int num, int expo
// return: int result
int CalExpo(int num, int expo);

// functions: self destruct
// argument:pboard
// return void;
void SelfDestruct(pboard B);

#endif //ASSIGNMENT_9024_BOARDADT_H
