/*
    created by SONGWEI z5198433
    this program is usd to find all the max longest ladders in the graph
    and all the words in the ladders should be in alphabetical order
    this program includes three parts:first is read all the word from stdin
    and put them into the dictionary, second part is use this dictionary to build
    a graph, third part is use the graph to find all the longest ladders
*/
#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include <ctype.h>
#include "Graph.h"
#include "Quack.h"

typedef struct dict *Dict;   // use a struct to store words dictionary
typedef struct route *Route; // use a struct to store routes

struct dict
{
    int numVertices;    // number of vertex     
    char **word;        // all words from stdin
};

struct route
{
    int numRoute;       // record the number of route
    int maxLength;      // record current max length
    int **allRoutes;    // store all routes
};

bool differByone(char *, char *);   // judge the differ one
Dict createDicFromStdin(void);      // read graph info from stdin
void showDict(Dict);                // show current dictionary
void readGraph(Graph, Dict);        // read graph info from dict
bool isInDict(Dict, char *, int);   // check current word if is in dict or not
void freeDict(Dict);                // free the dictionary mallocs

Route initializeRoute(void);        // initialize the route
void updateRoute(Route, int *, int);// update the existing routes
void addOneRoute(Route, int *, int);// add a new route to Route
Route clearAllRoute(Route);         // clear all the old routes
void showAllRoute(Route, Dict);     // print all the routes on screen
Route dfsFindRoute(Graph, Vertex, int, Route, Dict);// dfs to find all the possible longest routes
void allLongestLadders(Graph, Dict);// find the final longest routes
int nodeWithNoParents(Graph, Vertex);// use to find the node without parents


int main(void) {
    Dict d;                             // create a dict to store all the words
    d = createDicFromStdin();           // initialize the dict
    if (d->numVertices > 0){
        Graph g;                        // create a graph to store all conn state
        g = newGraph(d->numVertices);   // initialize the graph
        showDict(d);                    // show our dict
        readGraph(g, d);                // read info into graph from dict
        printf("Ordered Word Ladder Graph\n");
        showGraph(g);                   // show graph
        allLongestLadders(g, d);        // find all longest ladders and show them
        freeGraph(g);                   // free graph        
    }
    freeDict(d);	// free dictionary
    return EXIT_SUCCESS;
}


// case 1: change one letter, eg. barn->born
// case 2: adding or removing one letter, eg. band->brand
// case 1 or case 2: true, otherwise false
bool differByone(char *wordA, char *wordB)
{
    char replace = '*';     // use for replace the different letters in two words
    int firstWordLength = strlen(wordA);
    int secondWordLength = strlen(wordB);           // length of two string
    char *firstWord = NULL, *secondWord = NULL;     // define the copy version for two words
    int wordLongerLength = (firstWordLength > secondWordLength)?firstWordLength:secondWordLength;
    firstWord = malloc(sizeof(char) * (wordLongerLength + 2));
    secondWord = malloc(sizeof(char) * (wordLongerLength + 2));
    if (firstWord == NULL || secondWord == NULL){   // malloc memory failure
        fprintf(stderr, "differByone error: no memory\n");
        exit(EXIT_FAILURE);
    }
    strcpy(firstWord, wordA);                       // copy the original string wordA
    strcpy(secondWord, wordB);                      // copy the original string wordB
    if (firstWordLength - secondWordLength != 1 && \
        firstWordLength - secondWordLength != -1 && \
        firstWordLength - secondWordLength != 0){
        return false;       
    }
    if (strcmp(firstWord, secondWord) == 0){
        return false;
    }
    if (firstWordLength == secondWordLength){
        for (int i = 0; i < firstWordLength; i++)
        {                                           // now try to find the different position
            if (*(firstWord + i) != *(secondWord + i)){
                *(firstWord + i) = replace;         // replace it with a star symbol
                *(secondWord + i) = replace;        // replace it with a star symbol
                break;
            }
        }               
    }else{
        for (int i = 0; i < wordLongerLength; i++)
        {                                           // now try to find the different position
            if (*(firstWord + i) != *(secondWord + i)){
                if (i == firstWordLength || i == secondWordLength){
                    *(firstWord + i) = replace;         // replace it with a star symbol
                    *(secondWord + i) = replace;        // replace it with a star symbol
                    *(firstWord + i + 1) = '\0';
                    *(secondWord + i + 1) = '\0';                            
                }else{
                    if (firstWordLength > secondWordLength){
                        for ( ; i < firstWordLength; i++)
                        {
                            *(firstWord + i) = *(firstWord + i + 1);
                        }
                    }else{
                        for ( ; i < secondWordLength; i++)
                        {
                            *(secondWord + i) = *(secondWord + i + 1);
                        }                               
                    }
                }
                break;
            }
        }               
    }
    if (strcmp(firstWord, secondWord) == 0){    // now they should be completely same
        return true;
    }else{
        return false;
    }
    free(firstWord);    // free mallocs of the first word
    free(secondWord);   // free mallocs of the second word
    firstWord = NULL;   // set first word pointer as null
    secondWord = NULL;  // set second word pointer as null  
}

Dict createDicFromStdin(void)
{
    int lengthOfword = 0, numVertices = 0, doubleMemorySizeS = 1;
    int doubleMemorySizeW = 1;
    char ch, *singWord;
    singWord = NULL;
    Dict d = NULL;
    d = malloc(sizeof(struct dict));    // malloc memory for the new dictionary
    if (d == NULL){                     // out of memory
        fprintf(stderr, "createDicFromStdin:no memory\n");
        exit(EXIT_FAILURE);
    }
    d->word = malloc(sizeof(char *));
    if (d->word == NULL){
        fprintf(stderr, "createDicFromStdin:no memory\n");
        exit(EXIT_FAILURE);
    }
    while ((ch = getchar()) != EOF)
    {
        if (singWord == NULL){
            singWord = malloc(sizeof(char));
            if (singWord == NULL){
                fprintf(stderr, "createDicFromStdin:no memory\n");
                exit(EXIT_FAILURE);
            }           
        }
        if (!isspace(ch)){  // read not space from stdin
            lengthOfword++; // the length should plus 1
            doubleMemorySizeS = lengthOfword + 1;
            if (lengthOfword + 2 >= doubleMemorySizeS){
            	doubleMemorySizeS *= 2;
 				singWord = realloc(singWord, sizeof(char) * doubleMemorySizeS);           	
            }
            *(singWord + lengthOfword - 1) = ch;// add the new char to the word
            *(singWord + lengthOfword) = '\0';  // end of string add a '\0'
        }else{
            if (lengthOfword && !isInDict(d, singWord, numVertices)){// when meet space we stop and handle the word
                numVertices++;
                if (numVertices + 2 >= doubleMemorySizeW){
                	doubleMemorySizeW *= 2;
                	d->word = realloc(d->word, sizeof(char *) * doubleMemorySizeW);// double the memory size
                }
                d->word[numVertices - 1] = malloc(sizeof(char) * doubleMemorySizeS);
                if (d->word[numVertices - 1] == NULL){
                    fprintf(stderr, "createDicFromStdin:no memory\n");
                    exit(EXIT_FAILURE);
                }
                strcpy(d->word[numVertices - 1], singWord);
            }
            free(singWord);
            singWord = NULL;
            lengthOfword = 0;
        }
    }
    if (lengthOfword && !isInDict(d, singWord, numVertices)){// when meet space we stop and handle the word
        numVertices++;
        if (numVertices + 2 >= doubleMemorySizeW){
        	doubleMemorySizeW *= 2;	// double memory size
        	d->word = realloc(d->word, sizeof(char *) * doubleMemorySizeW);// double the memory size
        }
        d->word[numVertices - 1] = malloc(sizeof(char) * doubleMemorySizeS);
        if (d->word[numVertices - 1] == NULL){
            fprintf(stderr, "createDicFromStdin:no memory\n");
            exit(EXIT_FAILURE);
        }
        strcpy(d->word[numVertices - 1], singWord);
    }
    free(singWord);
    singWord = NULL;
    lengthOfword = 0;
    d->numVertices = numVertices;
    return d;   
} 

bool isInDict(Dict d, char *newWord, int numVertices)
{   // use to confirm if the new word is already in dict or not
    bool alreadyExist = false;
    for (int i = 0; i < numVertices; i++)
    {   // find the same word from the dict
        if (strcmp(d->word[i], newWord) == 0){
            alreadyExist = true;
        }
    }
    return alreadyExist;
}

void showDict(Dict d)
{	// show the dictionary we have read from the stdin
    if (d == NULL){
        fprintf(stderr, "showDict error: should create Dict first\n");
    }else{
        int numVertices = d->numVertices;
        printf("Dictionary\n");
        for (int i = 0; i < numVertices; i++)
        {
            printf("%d: %s\n", i, d->word[i]);
        }
    }
    return;
}

void freeDict(Dict d)
{	// free all mallocs for the dictionary
	if (d == NULL){// dictionary is not initialized
		fprintf(stderr, "freeDict: dictionary is not initialized\n");
	}else{
		for (int i = 0; i < d->numVertices; i++)
		{
			free(d->word[i]);// free every pointer for each word
		}
		free(d->word);	// free the word
		free(d);	// lastly free the dictionary
	}
	return;
}

void readGraph(Graph g, Dict d)
{
    Vertex v1 = 0, v2 = 0;
    int numVertices = d->numVertices;
    for (v1 = 0; v1 < numVertices; v1++)
    {                                   // visit first vertex
        for (v2 = 0; v2 < numVertices; v2++)
        {                               // visit second vertex
            Edge e = newEdge(v1, v2);   // create an Edge for them
            if (differByone(d->word[v1], d->word[v2]) && !isEdge(e, g)){// if first vertex connect second vertex
                insertEdge(e, g);       // insert this edge to graph
            }
        }
    }
}

Route initializeRoute(void)
{   // initialize the route
    Route r = NULL;
    r = malloc(sizeof(struct route));
    if (r == NULL){     // malloc failure: out of memory
        fprintf(stderr, "initializeRoute error: no memory\n");
        exit(EXIT_FAILURE);
    }
    r->allRoutes = malloc(sizeof(int *));
    if (r->allRoutes == NULL){
        fprintf(stderr, "initializeRoute error: no memory\n");
        exit(EXIT_FAILURE);
    }
    r->numRoute = 0;    // initial num of route is 0
    r->maxLength = 0;   // initial max length is 0
    return r;
}

void updateRoute(Route r, int*singleRoute, int curLength)
{
    if (r == NULL){
        fprintf(stderr, "addOneRoute error: route is not initialized\n");
    }else{
        if (curLength > r->maxLength){  // if current length longer than max length
            clearAllRoute(r);           // clear all the routes we have
            addOneRoute(r, singleRoute, curLength);
        }else if(curLength == r->maxLength){// if current length is same as max length
            addOneRoute(r, singleRoute, curLength);
        }
    }
    return;
}

Route clearAllRoute(Route r)
{   // clear all the routes we have if new route longer
    if (r == NULL){
        fprintf(stderr, "addOneRoute error: route is not initialized\n");
    }else{
        for (int i = 0; i < r->numRoute; i++)
        {   // every route should be delete
            free(r->allRoutes[i]);
            r->allRoutes[i] = NULL;
        }
        r->maxLength = 0;// current maxLength should set 0 again
        r->numRoute = 0;// current number of root
    }
    return r;   
}

void addOneRoute(Route r, int*singleRoute, int curLength)
{
    if (r == NULL){
        fprintf(stderr, "addOneRoute error: route is not initialized\n");
    }else{// before we add we need to check if the new route is existing or not
        int findSame = 1, sameNumber = 0, existSame = 0, doubleMemorySizeR = 2;
        for (int i = 0; i < r->numRoute; i++)
        {
            findSame = 1;
            for (int j = 0; j < r->maxLength; j++)
            {
                if (*(r->allRoutes[i] + j) != *(singleRoute + j))
                {
                    findSame = 0;
                    sameNumber++;
                    break;
                }
            }
            if (findSame){
                break;
            }
        }
        if (sameNumber < r->numRoute){
            existSame = 1;
        }
        if (!existSame){
            r->numRoute++;
            doubleMemorySizeR = r->numRoute + 1;
            r->allRoutes = realloc(r->allRoutes, sizeof(int *) * doubleMemorySizeR);
            r->allRoutes[r->numRoute - 1] = malloc(sizeof(int) * (curLength + 1));
            if (r->allRoutes[r->numRoute - 1] == NULL){
                fprintf(stderr, "addOneRoute error: no memory\n");
                exit(EXIT_FAILURE);
            }
            for (int i = 0; i < curLength; i++)
            {
                *(r->allRoutes[r->numRoute - 1] + i) = *(singleRoute + i); 
            }
            *(r->allRoutes[r->numRoute - 1] + curLength) = '\0'; // add end symbol;
            r->maxLength = curLength;
        }
    }
    return;
}

void showAllRoute(Route r, Dict d)
{
    if (r == NULL){
        fprintf(stderr, "showAllRoute error: route is not initialized\n");
    }else{
        int i = 0, j = 0;
        printf("Longest ladder length: %d\n", r->maxLength);
        printf("Longest ladders:\n");
        for (i = 0; i < r->numRoute && i < 99; i++)
        {
            printf("%2d: ", i + 1);
            for (j = 0; j < r->maxLength - 1; j++)
            {
                printf("%s -> ", d->word[*(r->allRoutes[i] + j)]);
            }
            printf("%s\n", d->word[*(r->allRoutes[i] + j)]);
        }
    }
    return;
}

int nodeWithNoParents(Graph g, Vertex rootV)
{   // this func is usd to determine if the current node have parent or not
    int rootVWithNoParent = 1;
    for (int w = 0; w < rootV; w++){
        Edge e = newEdge(rootV, w);// create an Edge for them
        if (isEdge(e, g)){
            rootVWithNoParent = 0;
            break;  
        }
    }
    return rootVWithNoParent;
}


Route dfsFindRoute(Graph g, Vertex rootV, int numberVertex, Route r, Dict d)
{
    int *singleRoute, length = 0, newLength = 0;
    int doubleMemorySizeS = 1, *depth; // use to record the current depth of route
    depth = malloc(sizeof(int) * (numberVertex + 1));
    for (int i = 0; i < numberVertex; i++)
    {                       //  initialize all depath of vertex as -1
        *(depth + i) = -1;
    }
    Quack q = NULL;
    q = createQuack();
    push(rootV, q);
    *(depth + rootV) = 0;   // the root vertex is 0
    singleRoute = malloc(sizeof(int));
    if (singleRoute == NULL){   // singRoute get no memory
        fprintf(stderr, "dfsFindRoute error: no memory\n");
        exit(EXIT_FAILURE);
    }
    while(!isEmptyQuack(q))
    {
        Vertex v = pop(q);
        for (Vertex w = numberVertex - 1; w > v; w--)
        {
            Edge e = newEdge(v, w); // create an Edge for them
            if (isEdge(e, g)){      // if my child nodes depth less or equal to my depth + 1
                if (*(depth + w) <= *(depth + v) + 1){
                    *(depth + w)= *(depth + v) + 1; // update my next nodes's depth
                    push(w, q);     // push all next nodes into quack
                }
            }
        }
        if (v == rootV){            // current node is the root node
            length++;               // length should plus 1
            if (length + 2 >= doubleMemorySizeS){
            	doubleMemorySizeS *= 2;	// if we need more memory, we double the memory size
 				singleRoute = realloc(singleRoute, sizeof(int) * doubleMemorySizeS);           	
            }
            *(singleRoute) = v;     // add the route to the current route
        }else{
            for (int indexW = length - 1; indexW >= 0; indexW--)
            {
                Edge e = newEdge(v, *(singleRoute + indexW));// create an Edge for them
                if (isEdge(e, g) && *(singleRoute + indexW) < v){
                    newLength = indexW + 2;
                    if (newLength <= length){   // if newlength of route bigger than old length
                        updateRoute(r, singleRoute, length);// update the old route
                    }
                    if (newLength + 2 >= doubleMemorySizeS){
                    	doubleMemorySizeS *= 2;// if need more memory we double the memory size
                    	singleRoute = realloc(singleRoute, sizeof(int) * doubleMemorySizeS);
                    }
                    *(singleRoute + indexW + 1) = v;
                    length = newLength;
                    break;
                }
            }
        }
    }
    updateRoute(r, singleRoute, length);// handle the last route        
    free(singleRoute);  // free the mallocs to the memory
    makeEmptyQuack(q);  // make empty of our quack
    destroyQuack(q);	// destroy quack completely
    return r;
}

void allLongestLadders(Graph g, Dict d)
{
    if (g == NULL){
        fprintf(stderr, "allLongestLadders error: graph is NULL\n");
    }else{
        int numberVertex = d->numVertices, curRootV = 0, noParent = 0;
        Route r = NULL;
        r = initializeRoute();
        for (curRootV = 0; curRootV < numberVertex; curRootV++)
        {// we only need visit the nodes without parents 
            noParent = nodeWithNoParents(g, curRootV);
            if (noParent){
                dfsFindRoute(g, curRootV, numberVertex, r, d);
            }
        }
        showAllRoute(r, d);
    }
}
