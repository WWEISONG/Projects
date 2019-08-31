//
// Created by SONG WEI z5198433 on 2019/6/15.
//

/*
 this program is to solve puzzle board problem, which gives you
two board, and you need to determine if the first one can be 
inferred from the second one. My program htime complexity is about
O(n^2), it should performance not so well if given a huge data input
 */

#include <stdio.h>
#include <stdlib.h>
#include "boardADT.h"

typedef struct board *pboard;

int main(void)
{
    pboard start, goal;
    start = CreateAndInit();  // create and init start
    goal = CreateAndInit();  // create and init goal

    start = GetConfig(start);    // config start
    goal = GetConfig(goal);     // config goal

    if ( !isValid(start, goal) ){       // we confirm is the inout is valid or not
        return EXIT_FAILURE;
    }
    
	display(start, goal);    // display start and goal as requirement
    ShowResult(start, goal); // get and show the final result
	
	SelfDestruct(start);
	SelfDestruct(goal);

    return EXIT_SUCCESS;
}
