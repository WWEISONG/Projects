# python 3
# Created by WEI SONG z5198433 02/08/2019
# this program is used to implemente the routing protocol
# the routing table built by Diskstra Algorithm

import sys, re, copy, time, os
from socket import *
from json import dumps, loads
from threading import Thread
import signal

UDP_IP_ADDRESS = '127.0.0.1'
UPDATE_INTERVAL = 1.0
HEARTBEAT_INTERVAL = 1.0
FAILURE_ROUTER_HANDLE_INTERVAL = 3.0
DIJKSTRA_ALGORITHM_INTERVAL = 30.0
INFINITE_DIS = 10000

def initializeRouter(argv, localGlobalView, localRouter, linkedRouter, localRouterLinkState, neighbourOnline):
	if len(argv) < 2:
		print('Usage: Lsr.py Configure file name')	# incorrect input from command
	else:
		configureFile, configureList = argv[1], []	# receive configureFile and list
		with open(configureFile) as file: 			# {'linkedRouterID: [cost, port, timestamp, alive]', }
			for line in file:						# read line by line
				line = line.replace('\n', '')		# replacee CRLF with NULL
				configureList.append(line)			# append every line into list
		tempList = re.split(r'[\s]', configureList[0])
		localRouter[tempList[0]] = [int(tempList[1]), int(configureList[1])] #info for local router
		for node in configureList[2:]:				# handle linked router's info
			tempList = re.split(r'[\s]', node)
			if len(tempList) >= 3:					# ID, cost, portnumber
				linkedRouter[tempList[0]] = [float(tempList[1]), int(tempList[2]), time.time(), 1]
				neighbourOnline[tempList[0]] = [1, time.time()]
		localRouterLinkState[list(localRouter.keys())[0]] = [linkedRouter, time.time(), 1]
		localGlobalView.append(localRouterLinkState)

def sendLinkState(localRouter, linkedRouter, localRouterLinkState, fixedLinkedRouter):
	"""send it's own link state every UPDATE_INTERVAL"""

	while True:
		localRouterLinkState[list(localRouter.keys())[0]] = [linkedRouter, time.time(), 1]
		encapsulatPacket = {}
		encapsulatPacket[list(localRouter.keys())[0]] = localRouterLinkState
		packet = dumps(encapsulatPacket)	
		for myNeighbourID in list(fixedLinkedRouter.keys()):
			UdpPort = fixedLinkedRouter[myNeighbourID][1]
			UdpAddress = (UDP_IP_ADDRESS, UdpPort)
			senderSocket = socket(family=AF_INET, type=SOCK_DGRAM)
			senderSocket.sendto(packet.encode('utf-8'), UdpAddress)
		time.sleep(UPDATE_INTERVAL)

def transmitPacket(localRouter, linkedRouter, data, fromRouter, fixedLinkedRouter):
	"""this func is used to transmit received data packet"""

	encapsulatPacket = {}											# we encapsulate packet and add sender ID
	encapsulatPacket[list(localRouter.keys())[0]] = data
	packet = dumps(encapsulatPacket)								# use JSON to encapsulate packet data
	for myNeighbourID in list(fixedLinkedRouter.keys()):			# send to all my meighbours except the one who send me this packet
		if myNeighbourID != fromRouter:
			UdpPort = fixedLinkedRouter[myNeighbourID][1]			# get my neighbour's port
			UdpAddress = (UDP_IP_ADDRESS, UdpPort)					# get my neighbour's IP address:127.0.0.1
			senderSocket = socket(family=AF_INET, type=SOCK_DGRAM)	# create a socket object
			senderSocket.sendto(packet.encode('utf-8'), UdpAddress)	# send the data packet out

def heartBeat(localRouter, linkedRouter, localRouterLinkState, fixedLinkedRouter):
	"""this func is used to send heartbeat message"""

	while True:
		heartBeatData = {}
		heartBeatData[list(localRouter.keys())[0]] = [1, time.time()] # heartbeat data includes a number 1 and current time
		encapsulatPacket = {}										  # encapsulate packet and add sender's ID
		encapsulatPacket[list(localRouter.keys())[0]] = heartBeatData
		packet = dumps(encapsulatPacket)
		for myNeighbourID in list(fixedLinkedRouter.keys()):		  # send to all my neighbours
			UdpPort = fixedLinkedRouter[myNeighbourID][1]			  # get my neighbour's port
			UdpAddress = (UDP_IP_ADDRESS, UdpPort)					  # get my neighbour's address
			senderSocket = socket(family=AF_INET, type=SOCK_DGRAM)
			senderSocket.sendto(packet.encode(), UdpAddress)
		time.sleep(HEARTBEAT_INTERVAL)								  # time sleep interval

# this is receive thread
# it's like a server waiting for packet from other nodes, the functions of the thread includes
# three parts: first is to get data from other nodes, second is to update this data into
# corresponding data container, third is to transmit the data to it's neighbours except the one
# who send this data to it.
def receiveLinkStateAndTransmit(localRouter, linkedRouter, localGlobalView, neighbourOnline, fixedLinkedRouter):
	"""this func is used to receive and handle data"""
	
	localIP = UDP_IP_ADDRESS								# local address is: 127.0.0.1
	localPort = localRouter[list(localRouter.keys())[0]][0]	# local port
	localAddress = (localIP, localPort)
	localReceiver = socket(family=AF_INET, type=SOCK_DGRAM)	# create the socket and the protocol is UDP
	localReceiver.bind(localAddress)						# bind the local address to this socket
	while True:
		packet, add = localReceiver.recvfrom(1024)			# listening...
		packetData = loads(packet.decode('utf-8', errors='ignore'))
		fromRouter = list(packetData.keys())[0]				# we extract the fromRoute ID from the packet
		data = packetData[fromRouter]						# we extract the real data from the packet
		newTimeStamp, newPacketID = list(data.values())[0][1], list(data.keys())[0]
		tempGlobalView = copy.deepcopy(localGlobalView)
		GlobalViewID = []									# list container used to store all links
		for localPacket in localGlobalView:
			GlobalViewID.append(list(localPacket.keys())[0])
		if type(list(data.values())[0][0]) == int:			# divide the packet as two types:1. heartbeat 2. link state
			if newPacketID in list(neighbourOnline.keys()):
				for myNeighbourID in list(neighbourOnline.keys()):
					if newPacketID == myNeighbourID \
						and newTimeStamp > neighbourOnline[myNeighbourID][1]: # if the timestamp is larger than the old we update
						neighbourOnline[myNeighbourID][1] = newTimeStamp
						neighbourOnline[myNeighbourID][0] += 1	# and we label our neighbour is still alive
						transmitPacket(localRouter, fixedLinkedRouter, data, fromRouter, fixedLinkedRouter)
			tempGlobalView = copy.deepcopy(localGlobalView)
			for localPacket in tempGlobalView:					# and then we transmit this packet
				if list(localPacket.keys())[0] == newPacketID \
					and newTimeStamp > list(localPacket.values())[0][1]:
					localGlobalView.remove(localPacket)
					list(localPacket.values())[0][1] = newTimeStamp
					list(localPacket.values())[0][2] += 1
					localGlobalView.append(localPacket)
					transmitPacket(localRouter, fixedLinkedRouter, data, fromRouter, fixedLinkedRouter)
		else:												# case the packet is real data
			if newPacketID in GlobalViewID:					# update the packet into our global view
				for localPacket in tempGlobalView:
					if list(localPacket.keys())[0] == newPacketID:
						localPacketTimeStamp = list(localPacket.values())[0][1]
						if newTimeStamp > localPacketTimeStamp:
							localGlobalView.remove(localPacket)
							localGlobalView.append(data)
							transmitPacket(localRouter, fixedLinkedRouter, data, fromRouter, fixedLinkedRouter)
			else:
				localGlobalView.append(data)
				transmitPacket(localRouter, fixedLinkedRouter, data, fromRouter, fixedLinkedRouter)

def handleRouterFailure(localRouter, neighbourOnline, linkedRouter, localGlobalView, localRouterLinkState, fixedLinkedRouter):
	"""this thread is used to handle failure routers and check the alive routers link state"""

	while True:
		time.sleep(FAILURE_ROUTER_HANDLE_INTERVAL)		# wait for every 3 sec to handle	
		tempGlobalView = copy.deepcopy(localGlobalView)
		for myNeighbourID in neighbourOnline:
			if neighbourOnline[myNeighbourID][0] == 0:	# neighbour is gone
				if myNeighbourID in linkedRouter:
					del linkedRouter[myNeighbourID]		# then should delete neighbour from link state
			else:
				if myNeighbourID not in linkedRouter:	# if my neighbours start again and update into link state
					linkedRouter[myNeighbourID] = fixedLinkedRouter[myNeighbourID]
					localRouterLinkState[list(localRouter.keys())[0]] = [linkedRouter, time.time(), 1]
			neighbourOnline[myNeighbourID][0] = 0
		for localPacket in tempGlobalView:
			if list(localPacket.keys())[0] == list(localRouter.keys())[0]:
				localGlobalView.remove(localPacket)
				localRouterLinkState[list(localRouter.keys())[0]] = [linkedRouter, time.time(), 1]
				localGlobalView.append(localRouterLinkState)
		tempGlobalView = copy.deepcopy(localGlobalView)			
		for localPacket in tempGlobalView:				# after a handle round we initialize neighbours again
			localGlobalView.remove(localPacket)
			if list(localPacket.values())[0][2] != 0:
				list(localPacket.values())[0][2] = 0	# all should be 0 again
				localGlobalView.append(localPacket)

def findLeastUncollectNodeID(cost, collect):
	"""used in Dijkstra algorithm to find the least cost of uncollected node"""

	costCopy, collectCopy = copy.deepcopy(cost), copy.deepcopy(collect)
	findThisNode = 0
	for node in collect:
		if collect[node] == 0:
			leastUncollectedNode = node
			findThisNode = 1
			break
	if findThisNode == 1:
		for currNodeID in cost:
			if cost[currNodeID] < cost[leastUncollectedNode] and \
				collect[currNodeID] == 0:
				leastUncollectedNode = currNodeID
		return leastUncollectedNode
	else:
		return 0

def DijkstraAlgorithm(sourceRouter, linkedRouter, localGlobalView):
	"""Dijkstra Algorithm to find the shortest paths"""
	
	cost, path = {}, {}
	collect, allRouterID = {}, []
	globalNodesCopy = copy.deepcopy(localGlobalView)
	globalNodesDic = {} # original globalNodes is a list, for convenient we now need dictionary
	linkedNodeCopy = copy.deepcopy(linkedRouter)
	for singleRouter in globalNodesCopy:
		allRouterID.append(list(singleRouter.keys())[0])
		globalNodesDic[list(singleRouter.keys())[0]] = singleRouter[list(singleRouter.keys())[0]]
    # important thing to initialize the cost
    # for directly connect router, update the real cost
    # but for indirectly connect router, update the inifinite positive value say 10000
	sourceRouterID, directLinkedRouterID = list(sourceRouter.keys())[0], list(linkedNodeCopy.keys())
	for routerID in allRouterID:
		if routerID == sourceRouterID:
			collect[routerID] = 1		# initialize the current router as 1
			cost[routerID] = 0			# the cost to itself is 0
			path[routerID] = 'source'	# we label it as 'source' router
		else:
			collect[routerID] = 0		# for other routers collect is 0
			path[routerID] = ''			# path is NULL...
			if routerID in directLinkedRouterID:
				cost[routerID] = linkedNodeCopy[routerID][0]
				path[routerID] = sourceRouterID
			else:
				cost[routerID] = INFINITE_DIS                  # a very big numer indicates None-Conn
				path[routerID] = ''                            # initialization path is null
	while True:
		currouterID = findLeastUncollectNodeID(cost, collect)
		if currouterID == 0:
			break
		else:
			collect[currouterID] = 1                           # then we collect it
			neighbourRouters = globalNodesDic[currouterID][0]  # and then we visit all it's neighbours
			for neighbour in list(neighbourRouters.keys()):
				if currouterID in cost and neighbour in cost and collect[neighbour] == 0 and \
					cost[currouterID] + neighbourRouters[neighbour][0] < cost[neighbour]:
					cost[neighbour] = round(cost[currouterID] + neighbourRouters[neighbour][0], 1)
					path[neighbour] = currouterID
	for singlePoint in list(path.keys()):
	    if not path[singlePoint]:
	        del path[singlePoint]
	        cost[singlePoint] = INFINITE_DIS
	        allRouterID.remove(singlePoint)
	print(f'I am router {sourceRouterID}')
	for routerID in allRouterID:
		if routerID != sourceRouterID:
			pathString, preRouterID = routerID, path[routerID]
			pathString += preRouterID
			while path[preRouterID] != 'source':               # if back to the source: stop
				preRouterID = path[preRouterID] 
				pathString += preRouterID
			print(f'Least cost path to router {routerID}: {pathString[::-1]} and the cost is {cost[routerID]}')

def runDijkstra(sourceRouter, linkedRouter, localGlobalView):
	"""run Dijkstra algoritm every 30 sec"""

	while True:
		time.sleep(DIJKSTRA_ALGORITHM_INTERVAL)
		DijkstraAlgorithm(sourceRouter, linkedRouter, localGlobalView)
		
def exitProgram(signal, frame):
	"""exit program through Ctrl C"""
	
	print(" you terminate the program")
	os._exit(0)

def main(argv):
	localGlobalView = []
	localRouter, linkedRouter = {}, {}
	localRouterLinkState, neighbourOnline = {}, {} 	# store local router global link state neighbourOnline = {'B':[timestamp, 1]}
	initializeRouter(argv, localGlobalView, localRouter, linkedRouter, localRouterLinkState, neighbourOnline)
	fixedLinkedRouter = copy.deepcopy(linkedRouter)
	
	signal.signal(signal.SIGINT, exitProgram)
	signal.signal(signal.SIGTERM, exitProgram)	
	sendTask = Thread(target=sendLinkState, args=(localRouter, linkedRouter, localRouterLinkState, fixedLinkedRouter))
	sendTask.start()
	receiveTask = Thread(target=receiveLinkStateAndTransmit, args=(localRouter, linkedRouter, localGlobalView,\
		neighbourOnline, fixedLinkedRouter))
	receiveTask.start()
	keepAlive = Thread(target=heartBeat, args=(localRouter, linkedRouter, localRouterLinkState, fixedLinkedRouter))
	keepAlive.start()
	handleFailure = Thread(target=handleRouterFailure, args=(localRouter, neighbourOnline, linkedRouter, localGlobalView,\
		localRouterLinkState, fixedLinkedRouter))
	handleFailure.start()
	runDijkstraAlgorithm = Thread(target=runDijkstra, args=(localRouter, linkedRouter, localGlobalView))
	runDijkstraAlgorithm.start()
	sendTask.join()
	receiveTask.join()
	keepAlive.join()
	handleFailure.join()
	runDijkstraAlgorithm.join()

if __name__ == '__main__':
	main(sys.argv)
	
	
	
	
	
	
	
	
