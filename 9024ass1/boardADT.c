//
// Created by SONGWEI z5198433 on 2019/6/20.
//

#include <stdio.h>
#include <ctype.h>
#include <stdlib.h>
#include <math.h>
#include "boardADT.h"

typedef struct board
{
    int *sequence;	// contain the sequence
    int index_b;   // record the index of 'b'
    int length;     // the length of sequence including a terminator at the end
}Board;

pboard CreateAndInit()
{
    pboard B;
    B = (Board *)malloc(sizeof(Board)); // allocate memory for pointer of Board
    if ( B == NULL ){        // if allocate failed, Exit_FAILURE
        fprintf(stderr, "No memory!");
        exit(EXIT_FAILURE);
    }
    B->sequence = (int *)malloc(sizeof(int)); // allocate memory for sequence
    B->index_b = -1;
    B->length = 0;

    return B;
}

pboard GetConfig(pboard B)
{
    int *p, cursor = 0, length = 0, index_b = -1;
    char *container, cur_char;
    p = (void *)malloc(sizeof(int) * length);          // define a pointer to contain all values
    container = (void *)malloc(sizeof(char) * cursor); // define a temporary container
    if ( p == NULL || container == NULL ){
        fprintf(stderr, "error: no memory");
        exit(EXIT_FAILURE);
    }
    while ( (cur_char = getchar()) != '\n')            // travel the sequence
    {
        if ( !isspace(cur_char ) ){                    // only not space is meaningful
            container = (char *)realloc(container, sizeof(char) * (cursor + 1));
            *(container + cursor) = cur_char;
            cursor += 1;                    // use a cursor to record the size of temporary container
        }else{
            if ( cursor ){            // if the container is not empty we calculate it
                int num = 0;
                if ( *container != 'b'){      // if it is not 'b' then should be number
                    for ( int i = 0; i < cursor; i++ )
                    {
                        num += (*(container + i) - '0') * CalExpo(10, cursor - i - 1);
                    }
                    p = (int *)realloc(p, sizeof(int) * (length + 1));   // realloc memory for new value
                    if ( num != 0 ){           // if num is valid, it is must not be 0
                        *(p + length) = num;
                        length += 1;
                    }
                }else{
                    if ( cursor == 1 ){        // we get 'b' and should only one char
                        p = (int *)realloc(p, sizeof(int) * (length + 1));
                        *(p + length) = *container;
                        index_b = length;
                        length += 1;
                    }
                }
                cursor = 0;
                free(container);
                container = NULL;
            }
        }
    }

    if ( cursor ){           // in order to handle the tail of the sequence
        int num = 0;
        if ( *container != 'b'){
            for ( int i = 0; i < cursor; i++ )
            {
                num += (*(container + i) - '0') * CalExpo(10, cursor - i - 1);
            }
            p = (int *)realloc(p, sizeof(int) * (length + 1));
            if ( num != 0 ){
                *(p + length) = num;
                length += 1;
            }
        }else{
            if ( cursor == 1 ){        // we get 'b' and should only one char
                p = (int *)realloc(p, sizeof(int) * (length + 1));
                *(p + length) = *container;
                index_b = length;
                length += 1;
            }
        }
        cursor = 0;
        free(container);
        container = NULL;
    }

    B->sequence = (int *)realloc(B->sequence, sizeof(int) * length );
    B->sequence = p;       // update the sequence
    B->index_b = index_b;  // update the index of 'b'
    B->length = length;    // update the size value

    return B;
}

int isValid(pboard start, pboard goal)
{
    if ( !isSameSize(start, goal) || !Findblank(start, goal) || \
    !AllNumbers(start, goal) || !isBoard(start) || !isBoard(goal)){
        return 0;
    }else{
        return 1;
    }
}

bool isSameSize(pboard start, pboard goal)
{
    if ( (int) start->length != (int) goal->length ){
    	fprintf(stdout, "Input invalid: length different of start and goal\n");
        return false;
    }else{
        return true;
    }
}

bool Findblank(pboard start, pboard goal)
{
    // two sequence should be both have 'b'
    if ( (char) *( start->sequence + start->index_b ) != 'b' \
			|| (char) *( goal->sequence + goal->index_b) != 'b'){
		fprintf(stdout, "Input invalid: 'b' missing\n");
        return false;
    }else{
        return true;
    }
}

bool AllNumbers(pboard start, pboard goal)
{
    int N = start->length, i = 0;
    int *hash_start = (int *)malloc(sizeof(int) * N); // build a hash index
    int *hash_goal = (int *)malloc(sizeof(int) * N);
    *hash_start = 1;
    *hash_goal = 1;
    for ( i = 0; i < N; i++ )
    {
        if ( i != start->index_b ){                    // build hash table for hash_start
            if ( *(start->sequence + i) < N ){
                *(hash_start + *(start->sequence + i)) = 1;
            }else{                                      // if number beyond N, return false directly
            	fprintf(stdout, "Input invalid: number should be smaller than size\n");
                return false;
            }
        }

        if( i != goal->index_b ){                   // bulid hash table for hash_goal
            if ( *(goal->sequence + i) < N ){   // if number beyond N, return false directly
                *(hash_goal + *(goal->sequence + i)) = 1;
            }else{
            	fprintf(stdout, "Input invalid: number should be smaller than size\n");
                return false;
            }
        }
    }
    for ( i = 0; i < N; i++ )
    {
        if ( i != start->index_b ){              // we judge start is valid number
            if ( *(start->sequence + i) >= N || *(hash_start + i) != 1 ){
            	fprintf(stdout, "Input invalid: data missing\n");
                return false;
            }
        }
        if ( i != goal->index_b ){              // we judge goal is valid number
            if ( *(goal->sequence + i) >= N || *(hash_goal + i) != 1){
            	fprintf(stdout, "Input invalid: data missing\n");
                return false;
            }
        }
    }

    free(hash_start);      // free pointer
    hash_start = NULL;
    free(hash_goal);       // free pointer
    hash_goal = NULL;

    return true;
}

bool isBoard(pboard B)
{
    int n = B->length, m = 0, s = 0, flag = 0; // we judge through if 1-n exit a number m,that m * m = n;
    int left = 1, right = n; // method is binary search
    while ( left <= right )
    {
        m = (left + right) / 2;
        s = m * m;
        if ( s == n ){      // if s == n then we find the sqrt we should return
        	flag = 1;
            return true;
        }else if ( s < n ){  // if s < n we should go right search
            left = m + 1;
        }else{				// if s > n we should go left search
            right = m - 1;
        }
    }
	if ( !flag ){
		fprintf(stdout, "Input invalid: not a N x N board\n");
		return false;		
	}
}
int disorder(pboard B)
{
    int sum = 0;
    for ( int i = 0; i < B->length; i++ ) // two loops to calculate the disorder one by one
    {
        if ( i != B->index_b ){			// be careful 'b' not count
            int order_tile = 0;
            for (int j = i + 1; j < B->length; j++ )
            {
                if ( *(B->sequence + i) > *(B->sequence + j) \
						&& j != B->index_b ){  // be careful 'b' not count
                    order_tile += 1;
                }
            }
            sum += order_tile;
        }
    }
    int row = 0;
    if ( (int)sqrt(B->length) % 2 == 0 ){         // even-parity should plus the row blank
        if ( !((B->index_b + 1) % (int)sqrt(B->length)) ){
            row = (int) ((B->index_b + 1) / sqrt((B->length)));
        }else{
            row = (int) ((B->index_b + 1) / sqrt((B->length))) + 1;
        }
    }

    return (sum + row);
}

int display(pboard start, pboard goal)
{
    int i = 0;
    fprintf(stdout, "start: ");
    for ( i = 0; i < start->length; i++ )       // show the result of start
    {
        if ( i != start->index_b ){             // we should consider 'b' specially
            fprintf(stdout, "%d ", (int) (*(start->sequence + i)));
        }else{
            fprintf(stdout, "%c ", (char) (*(start->sequence + i)));
        }
    }
    fprintf(stdout, "\ngoal: ");
    for ( i = 0; i < goal->length; i++ )        // show the result of goal
    {
        if ( i != goal->index_b ){             // we should consider 'b' specially
            fprintf(stdout, "%d ", (int) (*(goal->sequence + i)));
        }else{
            fprintf(stdout, "%c ", (char) (*(goal->sequence + i)));
        }
    }
    fprintf(stdout, "\n");

    return EXIT_SUCCESS;
}

void ShowResult(pboard start, pboard goal)
{
    if ( disorder(start) % 2 != disorder(goal) % 2 ){   // if they do not have the same parity
        fprintf(stdout, "unsolvable\n");                // then show unsolvable or it's solvable
    }else{
        fprintf(stdout, "solvable\n");
    }
    
    return;
}

int CalExpo(int num, int expo)
{
    // use to calculate the exponent of a number
    int i = 1, temp = num;
    if ( expo == 0 ){
        return 1;
    }
    for ( i = 1; i < expo; i++ )
    {
        num = num * temp;
    }

    return num;
}

void SelfDestruct(pboard B)
{
	if ( B == NULL ){
		fprintf(stdout, "Uninitialized board");
	}else{
		free(B->sequence);
		B->sequence = NULL;
		free(B);
		B = NULL;
	}	
	return;
}
