
import sys, os, signal
import socket

from threading import Thread 
from time import sleep

IP_ADDRESS = '127.0.0.1'

class CurrentPeer():

	peerID = -1
	firstSucc = 300
	secondSucc = 300
	firstSuccState = False
	secondSuccState = False
	interval = 10
	alive = True

	def __init__(self, peerID, firstSucc, secondSucc, interval):
		CurrentPeer.peerID = peerID
		CurrentPeer.firstSucc = firstSucc
		CurrentPeer.secondSucc = secondSucc
		CurrentPeer.interval = interval
		CurrentPeer.firstSuccState = True
		CurrentPeer.secondSuccState = True

	def _update_info(self, firstSucc, secondSucc):
		CurrentPeer.firstSucc = firstSucc
		CurrentPeer.secondSucc = secondSucc
	
	def launch_peer(self):
		pingServer = self.PingUDPServer("Server")
		pingServer.start()
		tcpServer = self.TCPServer("TCPServer")
		tcpServer.start()
		sleep(5)
		pingClint = self.PingClient("Client")
		pingClint.start()
		inputInfo = self.InputInfo("User-Input")
		inputInfo.start()

	@classmethod
	def send_udp_message(cls, message, address):
		sk = socket.socket(family=socket.AF_INET, type=socket.SOCK_DGRAM)
		sk.sendto(message, address)

	@classmethod
	def send_tcp_message(cls, message, address):
		sk = socket.socket(family=socket.AF_INET, type=socket.SOCK_STREAM)
		sk.connect(address)
		sk.send(message)

	class PingClient(Thread):
		def __init__(self, name):
			Thread.__init__(self)
			self.name = name
		
		def run(self):
			#  string --> bytes
			message = 'ping rquest message peerid'.encode('utf-8')
			while True:
				address = (IP_ADDRESS, 2000)
				CurrentPeer.send_udp_message(message=message, address=address)
				sleep(CurrentPeer.interval)
	
	class PingUDPServer(Thread):
		def __init__(self, name):
			Thread.__init__(self)
			self.name = name
		
		def run(self):
			sk = socket.socket(family=socket.AF_INET, type=socket.SOCK_DGRAM)
			sk.bind((IP_ADDRESS, 2000))
			while True:	
				# bytes --> string
				data, addr = sk.recvfrom(1024)
				print(data.decode('utf-8'))
	
	class TCPServer(Thread):
		def __init__(self, name):
			Thread.__init__(self)
			self.name = name
		
		def run(self):
			sk = socket.socket(family=socket.AF_INET, type=socket.SOCK_STREAM)
			sk.bind((IP_ADDRESS, 2000))
			sk.listen(10)
			while True:
				# waiting for data from sender
				conn, addr = sk.accept()
				data = conn.recv(1024)
				#  handle the data
				print(data.decode('utf-8'))\
				# make response according to the data
				conn.send()
				conn.close()
			sk.close()

	class InputInfo(Thread):
		def __init__(self, name):
			Thread.__init__(self)
			self.name = name 
		
		def run(self):
			while True:
				userInput = input()
				print(userInput)
				print()
				if userInput == "quit":
					print("bye!")
					print()
					os._exit(0)

def initCurPeer(argv):
	""" init current peer 
		1. peer id
		2. following two successors
		3. interval
	"""
	curPeer = CurrentPeer(1, 2, 3, 10)
	curPeer.launch_peer()

def exitProgram(signal, frame):

	print(" program is terminated")
	os._exit(0)

def main(argv):

	signal.signal(signal.SIGINT, exitProgram)
	signal.signal(signal.SIGTERM, exitProgram)
	initCurPeer(argv)



if __name__ == "__main__":
	main(sys.argv)


# store 2067
# read by peer: hash(2067) = 19, compare value with the peer ID (19 == 8?)
#--> forward this message,

