extends Node

var enet_connection: ENetConnection
var enet_peer: ENetPacketPeer
var poll_timer: float = 0.0
const POLL_INTERVAL: float = 0.016  # Poll at ~60 FPS, same as _process
var connection_timer: float = 0.0
const CONNECTION_TIMEOUT: float = 10.0  # 10 seconds timeout

# Called when the node enters the scene tree for the first time.
func _ready() -> void:
	setup_network_direct()

func _exit_tree() -> void:
	# Clean up ENet connection
	if enet_connection:
		enet_connection.destroy()
		enet_connection = null
		enet_peer = null


# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(delta: float) -> void:
	if enet_connection:
		# Check connection timeout
		connection_timer += delta
		if connection_timer > CONNECTION_TIMEOUT and enet_peer == null:
			print("Connection timeout - failed to connect within %.1f seconds" % CONNECTION_TIMEOUT)
			enet_connection = null
			return

		# Poll ENet connection at a controlled rate (not every frame to avoid spam)
		poll_timer += delta
		if poll_timer >= POLL_INTERVAL:
			poll_timer = 0.0

			# Poll for one event at a time (non-blocking)
			var event = enet_connection.service(0)

			if event.size() > 0:
				var event_type = event[0]

				match event_type:
					ENetConnection.EVENT_CONNECT:
						print("Connected to server!")
						connection_timer = 0.0  # Reset timer on successful connection
						if event.size() > 1:
							enet_peer = event[1] as ENetPacketPeer
					ENetConnection.EVENT_DISCONNECT:
						print("Disconnected from server")
						enet_peer = null
					ENetConnection.EVENT_RECEIVE:
						if event.size() > 1:
							var packet_peer = event[1] as ENetPacketPeer
							if packet_peer:
								var packet = packet_peer.get_packet()
								if packet:
									var message = packet.get_string_from_utf8()
									print("✅ Received: '%s' (size: %d)" % [message, packet.size()])
					ENetConnection.EVENT_NONE:
						pass  # No event, do nothing

# ---------- Setup direct ENet connection (bypassing multiplayer system) ----------
func setup_network_direct():
	# Create ENet connection directly without multiplayer layer
	enet_connection = ENetConnection.new()
	if not enet_connection:
		print("Failed to create ENetConnection")
		return

	# First create a client host (required before connecting)
	var error = enet_connection.create_host_bound("0.0.0.0", 0, 1, 2)  # 1 peer, 2 channels
	if error != OK:
		print("Failed to create ENet client host: ", error)
		enet_connection = null
		return

	# Now connect to Java server on port 7777
	var peer = enet_connection.connect_to_host("127.0.0.1", 7777, 2)  # 2 channels
	if peer == null:
		print("Failed to initiate connection to server")
		enet_connection.destroy()
		enet_connection = null
		return

	print("Connecting to Java server at 127.0.0.1:7777...")
	enet_peer = peer as ENetPacketPeer  # Store the peer for sending messages
	connection_timer = 0.0  # Reset connection timer

# ---------- Button signals ----------
#func _on_start_server_button_pressed():
	#setup_network(true)
#
#func _on_start_client_button_pressed():
	#setup_network(false)

func _on_send_message_button_pressed():
	if enet_peer and enet_connection:
		var msg = "Hi from Godot!"
		var packet = msg.to_utf8_buffer()
		if packet and packet.size() > 0:
			# Send directly via ENet peer on channel 0
			var error = enet_peer.send(0, packet, ENetPacketPeer.FLAG_RELIABLE)
			if error != OK:
				print("Failed to send message: ", error)
			else:
				print("Sent message: '%s'" % msg)
		else:
			print("Failed to create message packet")
	else:
		print("Not connected to server (peer: %s, connection: %s)" % [enet_peer, enet_connection])

# No longer needed - using direct ENet connection
