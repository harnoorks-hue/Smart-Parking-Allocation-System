
# Smart Parking DAA Module

from search_parking import display_search
from booking_queue import add_booking, process_booking, display_queue
from greedy_parking import greedy_allocation

while True:
    print("\n===== SMART PARKING DAA MODULE =====")
    print("1. Search Parking Slot (Binary Search)")
    print("2. Add Vehicle Booking")
    print("3. Process Booking (FIFO)")
    print("4. Display Booking Queue")
    print("5. Greedy Parking Allocation")
    print("6. Exit")

    choice = input("Enter Your Choice: ")

    if choice == "1":
        display_search()

    elif choice == "2":
        add_booking()

    elif choice == "3":
        process_booking()

    elif choice == "4":
        display_queue()

    elif choice == "5":
        greedy_allocation()

    elif choice == "6":
        print("Exiting Smart Parking DAA Module")
        break

    else:
        print("Invalid Choice")
