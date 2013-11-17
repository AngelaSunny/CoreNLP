package edu.stanford.nlp.neural;

import java.util.Iterator;
import java.util.Random;

import org.ejml.simple.SimpleMatrix;

/**
 * Includes a bunch of utility methods usable by projects which use
 * RNN, such as the parser and sentiment models.  Some methods convert
 * iterators of SimpleMatrix objects to and from a vector.  Others are
 * general utility methods on SimpleMatrix objects.
 *
 * @author John Bauer
 * @author Richard Socher
 * @author Thang Luong
 */
public class Utils {
  private Utils() {} // static methods only

  /**
   * Compute cosine distance between two column vectors.
   */
  public static double cosine(SimpleMatrix vector1, SimpleMatrix vector2){
    return dot(vector1, vector2)/(vector1.normF()*vector2.normF());
  }
  
  /**
   * Compute cosine distance between two column vectors.
   */
  public static double dot(SimpleMatrix vector1, SimpleMatrix vector2){
    if(vector1.numRows()==1){ // vector1: row vector, assume that vector2 is a row vector too 
      return vector1.mult(vector2.transpose()).get(0); 
    } else { // vector1: col vector, assume that vector2 is also a column vector.
      return vector1.transpose().mult(vector2).get(0);
    }
  }
  
  /**
   * Given a sequence of Iterators over SimpleMatrix, fill in all of
   * the matrices with the entries in the theta vector.  Errors are
   * thrown if the theta vector does not exactly fill the matrices.
   */
  public static void vectorToParams(double[] theta, Iterator<SimpleMatrix> ... matrices) {
    int index = 0;
    for (Iterator<SimpleMatrix> matrixIterator : matrices) {
      while (matrixIterator.hasNext()) {
        SimpleMatrix matrix = matrixIterator.next();
        int numElements = matrix.getNumElements();
        for (int i = 0; i < numElements; ++i) {
          matrix.set(i, theta[index]);
          ++index;
        }
      }
    }
    if (index != theta.length) {
      throw new AssertionError("Did not entirely use the theta vector");
    }
  }

  /**
   * Given a sequence of iterators over the matrices, builds a vector
   * out of those matrices in the order given.  Asks for an expected
   * total size as a time savings.  AssertionError thrown if the
   * vector sizes do not exactly match.
   */
  public static double[] paramsToVector(int totalSize, Iterator<SimpleMatrix> ... matrices) {
    double[] theta = new double[totalSize];
    int index = 0;
    for (Iterator<SimpleMatrix> matrixIterator : matrices) {
      while (matrixIterator.hasNext()) {
        SimpleMatrix matrix = matrixIterator.next();
        int numElements = matrix.getNumElements();
        //System.out.println(Integer.toString(numElements)); // to know what matrices are
        for (int i = 0; i < numElements; ++i) {
          theta[index] = matrix.get(i);
          ++index;
        }
      }
    }
    if (index != totalSize) {
      throw new AssertionError("Did not entirely fill the theta vector: expected " + totalSize + " used " + index);
    }
    return theta;
  }

  /**
   * Given a sequence of iterators over the matrices, builds a vector
   * out of those matrices in the order given.  The vector is scaled
   * according to the <code>scale</code> parameter.  Asks for an
   * expected total size as a time savings.  AssertionError thrown if
   * the vector sizes do not exactly match.
   */
  public static double[] paramsToVector(double scale, int totalSize, Iterator<SimpleMatrix> ... matrices) {
    double[] theta = new double[totalSize];
    int index = 0;
    for (Iterator<SimpleMatrix> matrixIterator : matrices) {
      while (matrixIterator.hasNext()) {
        SimpleMatrix matrix = matrixIterator.next();
        int numElements = matrix.getNumElements();
        for (int i = 0; i < numElements; ++i) {
          theta[index] = matrix.get(i) * scale;
          ++index;
        }
      }
    }
    if (index != totalSize) {
      throw new AssertionError("Did not entirely fill the theta vector: expected " + totalSize + " used " + index);
    }
    return theta;
  }

  /**
   * Returns a sigmoid applied to the input <code>x</code>.
   */
  public static double sigmoid(double x) {
    return 1.0 / (1.0 + Math.exp(-x));
  }

  /**
   * Applies softmax to all of the elements of the matrix.  The return
   * matrix will have all of its elements sum to 1.  If your matrix is
   * not already a vector, be sure this is what you actually want.
   */
  public static SimpleMatrix softmax(SimpleMatrix input) {
    SimpleMatrix output = new SimpleMatrix(input);
    for (int i = 0; i < output.numRows(); ++i) {
      for (int j = 0; j < output.numCols(); ++j) {
        output.set(i, j, Math.exp(output.get(i, j)));
      }
    }
    double sum = output.elementSum();
    // will be safe, since exp should never return 0
    return output.scale(1.0 / sum); 
  }

  /**
   * Applies log to each of the entries in the matrix.  Returns a new matrix.
   */
  public static SimpleMatrix elementwiseApplyLog(SimpleMatrix input) {
    SimpleMatrix output = new SimpleMatrix(input);
    for (int i = 0; i < output.numRows(); ++i) {
      for (int j = 0; j < output.numCols(); ++j) {
        output.set(i, j, Math.log(output.get(i, j)));
      }
    }
    return output;
  }

  /**
   * Applies tanh to each of the entries in the matrix.  Returns a new matrix.
   */
  public static SimpleMatrix elementwiseApplyTanh(SimpleMatrix input) {
    SimpleMatrix output = new SimpleMatrix(input);
    for (int i = 0; i < output.numRows(); ++i) {
      for (int j = 0; j < output.numCols(); ++j) {
        output.set(i, j, Math.tanh(output.get(i, j)));
      }
    }
    return output;
  }

  /**
   * Applies the derivative of tanh to each of the elements in the vector.  Returns a new matrix.
   */
  public static SimpleMatrix elementwiseApplyTanhDerivative(SimpleMatrix input) {
    SimpleMatrix output = new SimpleMatrix(input.numRows(), input.numCols());
    output.set(1.0);
    output = output.minus(input.elementMult(input));
    return output;
  }

  /**
   * Concatenates several column vectors into one large column
   * vector, adds a 1.0 at the end as a bias term
   */
  public static SimpleMatrix concatenateWithBias(SimpleMatrix ... vectors) {
    int size = 0;
    for (SimpleMatrix vector : vectors) {
      size += vector.numRows();
    }
    // one extra for the bias
    size++;

    SimpleMatrix result = new SimpleMatrix(size, 1);
    int index = 0;
    for (SimpleMatrix vector : vectors) {
      result.insertIntoThis(index, 0, vector);
      index += vector.numRows();
    }
    result.set(index, 0, 1.0);
    return result;
  }

  /**
   * Concatenates several column vectors into one large column vector
   */
  public static SimpleMatrix concatenate(SimpleMatrix ... vectors) {
    int size = 0;
    for (SimpleMatrix vector : vectors) {
      size += vector.numRows();
    }

    SimpleMatrix result = new SimpleMatrix(size, 1);
    int index = 0;
    for (SimpleMatrix vector : vectors) {
      result.insertIntoThis(index, 0, vector);
      index += vector.numRows();
    }
    return result;
  }

  /**
   * Returns a vector with random Gaussian values, mean 0, std 1
   */
  public static SimpleMatrix randomGaussian(int numRows, int numCols, Random rand) {
    SimpleMatrix result = new SimpleMatrix(numRows, numCols);
    for (int i = 0; i < numRows; ++i) {
      for (int j = 0; j < numCols; ++j) {
        result.set(i, j, rand.nextGaussian());
      }
    }
    return result;
  }
}

