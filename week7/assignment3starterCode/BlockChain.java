import java.util.ArrayList;
import java.util.HashMap;
// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    private BlockNode highestNode;
    private TransactionPool txPool;
    private UTXOPool utxoPool;
    private HashMap<byte[], BlockNode> nodes;
		
    private class BlockNode {
      public Block block;
      public int height;

      private UTXOPool utxoPool;
      
      public byte[] getHash() {
        return block.getHash();
      }

      public BlockNode(Block block, BlockNode parent, UTXOPool utxoPool) {
        this.block = block;
        this.utxoPool = utxoPool;
        if (parent != null) {
          height = parent.height + 1;
        } else {
        } 
      }
    }
    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
			nodes = new HashMap<>();
			utxoPool = new UTXOPool();
      txPool = new TransactionPool();
      highestNode = new BlockNode(genesisBlock, null, utxoPool);
      nodes.put(highestNode.getHash(), highestNode);
      addCoinbaseTXToUTXOPool(genesisBlock, utxoPool);
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
      return highestNode.block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
      return highestNode.utxoPool;
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
      return txPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
      byte[] prevBlockHash = block.getPrevBlockHash();
      if (prevBlockHash == null) {
        return false;
      }
      BlockNode parentBlockNode = nodes.get(prevBlockHash);
      if (parentBlockNode == null) {
        return false;
      }
      TxHandler txHandler = new TxHandler(utxoPool);
      ArrayList<Transaction> allTxArrayList = block.getTransactions();
      Transaction[] allTxs = allTxArrayList.toArray(new Transaction[allTxArrayList.size()]);
      Transaction[] validTxs = txHandler.handleTxs(allTxs);
      int newBlockNodeHeight = parentBlockNode.height;
      if (validTxs.length != allTxs.length) {
        return false;
      }
      if (newBlockNodeHeight < highestNode.height - CUT_OFF_AGE) {
        return false;
      }
      addCoinbaseTXToUTXOPool(block, txHandler.getUTXOPool());
      BlockNode newBlockNode = new BlockNode(block, parentBlockNode, utxoPool);
      nodes.put(newBlockNode.getHash(), newBlockNode);
      if (newBlockNodeHeight > highestNode.height) {
        highestNode = newBlockNode;
      }
      return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
      txPool.addTransaction(tx);
    }

    private void addCoinbaseTXToUTXOPool(Block block, UTXOPool utxoPool) {
      Transaction coinbaseTx = block.getCoinbase();
      for (int i = 0; i < coinbaseTx.numOutputs(); i++) {
        utxoPool.addUTXO(new UTXO(coinbaseTx.getHash(), i), coinbaseTx.getOutput(i));
      }
    }
}
