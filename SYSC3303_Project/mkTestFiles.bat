# Create some test files

# Empty file
fsutil file createnew empty.test 0

# Less than one block
fsutil file createnew small.test 255

# Exactly one block
fsutil file createnew oneblock.test 512

# Many blocks (10.5KB)
fsutil file createnew tenk.test 10500

# Large size (10MB)
fsutil file createnew meg.test 10000000